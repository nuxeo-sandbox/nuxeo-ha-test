//
// (C) Copyright 2006-2017 Nuxeo (http://nuxeo.com/) and others.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
//
//

package cmd

import (
	"context"
	"fmt"
	log "github.com/Sirupsen/logrus"
	homedir "github.com/mitchellh/go-homedir"
	"github.com/nuxeo-sandbox/nuxeo-ha-test/k8s-chaos-monkey/monkey"
	"github.com/spf13/cobra"
	"github.com/spf13/viper"
	"os"
	"os/signal"
	"runtime/pprof"
	"syscall"
)

var (
	cfgFile       string
	labelSelector string
	namespace     string
	killPeriod    int
	mainContext   context.Context
)

// RootCmd represents the base command when called without any subcommands
var RootCmd = &cobra.Command{
	Use:   "k8s-chaos-monkey",
	Short: "Run k8s Monkey",
	Long: `k8s monkey is to be runned in a k8s environment to 
kill pods randomly.`,
	// Uncomment the following line if your bare application
	// has an action associated with it:
	Run: func(cmd *cobra.Command, args []string) {

		conf := monkey.Conf{
			IncludeLabelSelector: viper.GetString("label_selector"),
			KillPeriodInSeconds:  viper.GetInt("killperiod"),
			NameSpace:            viper.GetString("namespace"),
		}

		monkey.Run(mainContext, &conf)
	},
}

// Execute adds all child commands to the root command and sets flags appropriately.
// This is called by main.main(). It only needs to happen once to the rootCmd.
func Execute() {
	var cancel context.CancelFunc
	mainContext, cancel = context.WithCancel(context.Background())
	handleSignals(cancel)

	if err := RootCmd.Execute(); err != nil {
		fmt.Println(err)
		os.Exit(1)
	}
}

func init() {
	cobra.OnInitialize(initConfig)

	// Here you will define your flags and configuration settings.
	// Cobra supports persistent flags, which, if defined here,
	// will be global for your application.
	RootCmd.PersistentFlags().StringVar(&cfgFile, "config", "", "config file (default is $HOME/.k8s-chaos-monkey)")
	RootCmd.PersistentFlags().StringVarP(&labelSelector, "label", "l", "", "label selector for pods to be deleted")
	RootCmd.PersistentFlags().StringVarP(&namespace, "namespace", "n", "", "namespace in which the pods will be deleted")
	RootCmd.PersistentFlags().IntVarP(&killPeriod, "killperiod", "k", 60, "period during the monkey sleeps and rests")

	viper.BindPFlag("label_selector", RootCmd.PersistentFlags().Lookup("label"))
	viper.BindPFlag("namespace", RootCmd.PersistentFlags().Lookup("namespace"))
	viper.BindPFlag("killperiod", RootCmd.PersistentFlags().Lookup("killperiod"))

}

// initConfig reads in config file and ENV variables if set.
func initConfig() {

	if cfgFile != "" {
		// Use config file from the flag.
		viper.SetConfigFile(cfgFile)
	} else {
		// Find home directory.
		home, err := homedir.Dir()
		if err != nil {
			fmt.Println(err)
			os.Exit(1)
		}

		// Search config in home directory with name ".k8s-chaos-monkey" (without extension).
		viper.AddConfigPath(home)
		viper.SetEnvPrefix("k8smonkey")
		viper.SetConfigName(".k8s-chaos-monkey")
	}

	viper.AutomaticEnv() // read in environment variables that match
	// If a config file is found, read it in.
	if err := viper.ReadInConfig(); err == nil {
		log.Infof("Using config file:", viper.ConfigFileUsed())
	}
}

func handleSignals(cancel context.CancelFunc) {
	signals := make(chan os.Signal)
	signal.Notify(signals, os.Interrupt, syscall.SIGTERM)
	signal.Notify(signals, os.Interrupt, syscall.SIGUSR1)
	signal.Notify(signals, os.Interrupt, syscall.SIGUSR2)

	go func() {
		isProfiling := false

		defer func() {
			if isProfiling {
				pprof.StopCPUProfile()
			}
		}()

		for {
			sig := <-signals
			switch sig {
			case syscall.SIGTERM, syscall.SIGINT:
				log.Infof("Gracefully Shutting down...")
				cancel()
				os.Exit(0)
			case syscall.SIGUSR1:
				pprof.Lookup("goroutine").WriteTo(os.Stdout, 2)
			case syscall.SIGUSR2:
				if !isProfiling {
					f, err := os.Create("/tmp/chaos.profile")
					if err != nil {
						log.Fatal(err)
					} else {
						pprof.StartCPUProfile(f)
						isProfiling = true
					}
				} else {
					pprof.StopCPUProfile()
					isProfiling = false
				}

			}
		}

	}()
}
