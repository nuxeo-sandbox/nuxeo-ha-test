# k8s-chaos-monkey

Once launched in a Kubernetes namespace, that utility acts as a [Chaos Monkey]() and delete randomly pods based on the selector that is specified as an environment variable.

The pod can be deployed like that in Kubernetes:


		kind: ConfigMap
		metadata:
		  name: chaos-monkey-config
		apiVersion: v1
		data:
		  label-selector: destroy=me
		  killperiod: "120"
          
		---
        
		apiVersion: extensions/v1beta1
		kind: Deployment
		metadata:
		  name: chaos-monkey
		spec:
		  replicas: 1
		  template:
		    metadata:
		      labels:
		        component: chaos        
		    spec:		      
		      serviceAccountName: chaos-monkey
		      containers:
		      - name: chaos-monkey       
		        image: chaos-monkey:latest
		        imagePullPolicy: Always
		        env:
		        - name: K8SMONKEY_NAMESPACE
		          valueFrom:
		            fieldRef:
		              fieldPath: metadata.namespace
		        - name: "K8SMONKEY_LABEL_SELECTOR"
		          valueFrom: 
		            configMapKeyRef:
		              name: chaos-monkey-config
		              key: label-selector
                    
		        - name: "K8SMONKEY_KILLPERIOD"
		          valueFrom: 
		            configMapKeyRef:
		              name: chaos-monkey-config
		              key: killperiod


You also have to check that the `serviceAccount`  has the right to delete Pod of the current namespace.