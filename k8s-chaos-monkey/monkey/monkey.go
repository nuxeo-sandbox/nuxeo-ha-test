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

package monkey

import (
	"context"
	log "github.com/Sirupsen/logrus"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
	"k8s.io/client-go/kubernetes"
	"k8s.io/client-go/rest"
	"math/rand"
	"time"
)

//Conf contains the configuration for the monkey
type Conf struct {
	IncludeLabelSelector string
	KillPeriodInSeconds  int
	NameSpace            string
}

var conf *Conf

// Run is the entry point to run a monkey
func Run(ctx context.Context, c *Conf) {
	conf = c
	log.Infof("Monkey is starting")
	log.Infof("  Label selector: %s", conf.IncludeLabelSelector)
	log.Infof("  KillPeriod: %d", conf.KillPeriodInSeconds)
	log.Infof("  Namespace: %s", conf.NameSpace)

	rand.Seed(time.Now().Unix()) // initialize global pseudo random generator

	clientset, err := getK8sClient()
	if err != nil {
		log.Errorf("Unable to get k8s client: %s", err)
		return
	}

	tickerChan := time.NewTicker(time.Second * time.Duration(conf.KillPeriodInSeconds)).C

	for {
		select {
		case <-tickerChan:
			killPod(clientset)
		case <-ctx.Done():
			log.Info("Monkey is stopping")
			return
		}
	}

}

func getK8sClient() (*kubernetes.Clientset, error) {
	config, err := rest.InClusterConfig()
	if err != nil {
		return nil, err
	}

	// creates the clientset
	clientset, err := kubernetes.NewForConfig(config)
	if err != nil {
		return nil, err
	}
	return clientset, nil

}

func killPod(clientset *kubernetes.Clientset) {
	podAPI := clientset.CoreV1().Pods(conf.NameSpace)
	pods, err := podAPI.List(metav1.ListOptions{LabelSelector: conf.IncludeLabelSelector})
	if err != nil {
		log.Errorf("Unable to get list of pods: %s", err.Error())
	}

	if len(pods.Items) == 0 {
		log.Info("No suitable pod found for our monkey :-(")
		return
	}

	pod := pods.Items[rand.Intn(len(pods.Items))]
	log.Infof("Pod %s has been selected as a toy for our monkey", pod.GetName())

	gracePeriod := int64(0)
	err = podAPI.Delete(pod.GetName(), &metav1.DeleteOptions{GracePeriodSeconds: &gracePeriod})
	if err != nil {
		log.Errorf("Unable to delete pod %s: %s", pod.GetName(), err.Error())
	} else {
		log.Infof("Monkey played with pod %s and broke it :-)", pod.GetName())
	}

}
