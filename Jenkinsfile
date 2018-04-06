#!/usr/bin/groovy

//Permissions for this library have to be granted in Jenkins
import groovy.json.JsonSlurperClassic
import hudson.FilePath
import org.eclipse.jgit.transport.URIish


podTemplate(label: 'mypod',
    volumes: [
        hostPathVolume(hostPath: '/var/run/docker.sock', mountPath: '/var/run/docker.sock'),
        secretVolume(secretName: 'registry-account', mountPath: '/var/run/secrets/registry-account'),
        configMapVolume(configMapName: 'registry-config', mountPath: '/var/run/configs/registry-config')
    ],
    containers: [
        containerTemplate(name: 'kubectl', image: 'lachlanevenson/k8s-kubectl', ttyEnabled: true, command: 'cat'),
        containerTemplate(name: 'docker' , image: 'docker:17.06.1-ce', ttyEnabled: true, command: 'cat'),
        containerTemplate(name: 'helm', image: 'lachlanevenson/k8s-helm:v2.7.2', command: 'cat', ttyEnabled: true),
        containerTemplate(name: 'maven', image: 'maven:3.3.9-jdk-8-alpine', ttyEnabled: true, command: 'cat'),
        containerTemplate(name: 'bxpr', image: 'mycluster.icp:8500/default/helm-bx-icp:2.1.0.2', ttyEnabled: true, command: 'cat')
  ]) {

    node('mypod') {

        /*
        Gather full path data for chart
         */
        def pwd = pwd()
        checkout scm

        //Read configuration file
        //Inspired by lachie83
        def configfile = readFile('Jenkinsfile.json')
        def config = new groovy.json.JsonSlurperClassic().parseText(configfile)
        println ">>> Pipeline Config ==> ${config}"

        // read cert and key
        def key = readFile('helm-cert/key.pem')
        def cert = readFile('helm-cert/cert.pem')
        
        if(!config.pipeline.enabled){
          println ">>> Pipeline disabled"
          return
        }

        def chart_dir = config.release.chart_dir

        /*
        Testing Kubectl
         */
        container('kubectl'){
          sh 'echo ">>> Testing kubectl"'
          sh 'kubectl get nodes'
        }

        container('helm'){
          sh 'echo ">>> Running Helm cli Test"'
          // copy key and cert files 
          sh 'cp *.pem /'
            
          sh 'helm init'
          sh 'helm list --tls'
        }

        stage ('Test helm Chart'){
            container('helm'){
              echo ">>> Running helm lint ${chart_dir}"
              sh "helm lint $chart_dir"
            }
        }

        stage('Maven Build'){
            container('maven'){
                // Maven Install here
                // mvn install -Pwlp-install -Dwlp.install.dir=/Users/oliverlucht/bluemix/liberty/wlp
                // sh 'mvn -B -DskipTests clean package'
                echo ">>> Running Maven build"
                sh 'mvn install -Pwlp-install'
                echo "Done Maven build"
            }
        }

        container('docker') {
            stage('Build Docker Image') {
              if(config.image.registry_type == "dockerhub"){
                withCredentials([[$class: 'UsernamePasswordMultiBinding', credentialsId: 'docker-hub-credentials',
                       usernameVariable: 'DOCKER_USER']]) {
                sh """
                #!/bin/bash
                docker build -t ${DOCKER_USER}/${config.image.image_name}:${env.BRANCH_NAME}-${env.BUILD_NUMBER} ${config.image.build_path}
                """
                }
              } else {
                sh """
                #!/bin/bash
                NAMESPACE=`cat /var/run/configs/registry-config/namespace`
                REGISTRY=`cat /var/run/configs/registry-config/registry`
                docker build -t \${REGISTRY}/\${NAMESPACE}/${config.image.image_name}:${env.BRANCH_NAME}-${env.BUILD_NUMBER} ${config.image.build_path}
                """
              }
            }
            stage('Test image') {
                 /* Ideally, we would run a test framework against our image.
                  */
                  sh 'echo ">>> No Test Framework specified"'
            }
            stage('Push Docker Image to Registry') {
                /*
                Using Jenkins specified Credentials for Docker Login
                 */
                 if(config.image.publish){
                    if(config.image.registry == "dockerhub"){
                      withCredentials([[$class: 'UsernamePasswordMultiBinding', credentialsId: 'docker-hub-credentials',
                             usernameVariable: 'DOCKER_USER', passwordVariable: 'DOCKER_PASSWORD']]) {
                        sh """
                        #!/bin/bash
                        mkdir -p /etc/docker/certs.d/mycluster.icp:8500/
                        cp ca.crt /etc/docker/certs.d/mycluster.icp:8500/
                        
                        docker login -u=${DOCKER_USER} -p=${DOCKER_PASSWORD}
                        docker push ${DOCKER_USER}/${config.image.image_name}:${env.BRANCH_NAME}-${env.BUILD_NUMBER}
                        """
                      }
                    } else {
                      sh """
                      #!/bin/bash
                      NAMESPACE=`cat /var/run/configs/registry-config/namespace`
                      REGISTRY=`cat /var/run/configs/registry-config/registry`

                      set +x
                      DOCKER_USER=`cat /var/run/secrets/registry-account/username`
                      DOCKER_PASSWORD=`cat /var/run/secrets/registry-account/password`
                      
                      mkdir -p /etc/docker/certs.d/mycluster.icp:8500/
                      cp ca.crt /etc/docker/certs.d/mycluster.icp:8500/
                      
                      docker login -u=\${DOCKER_USER} -p=\${DOCKER_PASSWORD} \${REGISTRY}
                      set -x
                      docker push \${REGISTRY}/\${NAMESPACE}/${config.image.image_name}:${env.BRANCH_NAME}-${env.BUILD_NUMBER}
                      """
                    }

                } else {
                  sh 'echo ">>> Publish Docker image deactivated in Jenkinsfile.json"'
                }
            }
        }

        if (env.BRANCH_NAME == 'master') {
          container('bxpr'){

            stage('Initalize Helm'){
              sh 'echo ">>> Initializing Helm..."'
              sh 'cp *.pem /'
              sh 'cp *.pem /home/jenkins/.helm/'
              sh 'helm init'
              sh 'helm list --tls'
              sh 'helm version --tls'
            }

            /*
            Initial Install creates a new Helm Deployment, otherwise a deployment is upgraded
             */

            stage('Helm Deployment'){

              /*
              Set Image variables
               */
              def REPOSITORY
              if(config.image.registry == "dockerhub"){
                withCredentials([[$class: 'UsernamePasswordMultiBinding', credentialsId: 'docker-hub-credentials',
                       usernameVariable: 'DOCKER_USER']]) {
                       REPOSITORY = "${DOCKER_USER}/${config.image.image_name}"
                }
              } else {
                def NAMESPACE = sh(script: 'cat /var/run/configs/registry-config/namespace', returnStdout: true)
                def REGISTRY = sh(script: 'cat /var/run/configs/registry-config/registry', returnStdout: true)
                REPOSITORY = "${REGISTRY}/${NAMESPACE}/${config.image.image_name}"
              }

              def TAG = "${env.BRANCH_NAME}-${env.BUILD_NUMBER}"

              if(config.release.dryrun){
                if(config.release.initialInstall){
                  sh """
                    #!/bin/bash
                    echo ">>> Installing new Helm Deployment Dryrun"
                    cp *.pem /
                    helm install ${config.release.chart_dir} --set image.repository=${REPOSITORY},image.tag=${TAG} --dry-run --name ${config.release.name} --tls
                  """
                } else {
                  sh """
                    #!/bin/bash
                    echo ">>> Upgrading Helm Deployment Dryrun"
                    helm upgrade ${config.release.name} ${config.release.chart_dir} --set image.repository=${REPOSITORY},image.tag=${TAG} --dry-run 
                  """
                }
              } else {
                if(config.release.initialInstall){
                  sh """
                    #!/bin/bash
                    echo ">>> Installing new Helm Deployment"                 
                    helm install ${config.release.chart_dir} --set image.repository=${REPOSITORY},image.tag=${TAG} --name ${config.release.name} --tls

                  """
                } else {
                  sh """
                    #!/bin/bash
                    echo ">>> Upgrading Helm Deployment1"
                    bx pr -u admin -p admin -a https://mycluster.icp:8443 -c id-icp-account
                    echo ">>> Upgrading Helm Deployment1.5"
                    cp *.pem /
                    cp *.pem /home/jenkins/.helm/
                    echo ">>> Upgrading Helm Deployment2"
                    ls -l /
                    echo ">>> Upgrading Helm Deployment3"
                    helm init
                    echo ">>> Upgrading Helm Deployment4"
                    helm list --tls
                    echo ">>> Upgrading Helm Deployment5"
                    helm version --tls
                    echo ">>> Upgrading Helm Deployment6"
                    bx pr login
                    chmod +x helm_icp
                    helm upgrade ${config.release.name} ${config.release.chart_dir} --set image.repository=${REPOSITORY},image.tag=${TAG} --tls
                    echo ">>> Upgrading Helm Deployment7"
                  """
                }
              }


            if(config.release.test){
              stage('Testing Helm Release'){
                sh 'echo ">>> Running Helm Test"'
                sh "helm test ${config.release.name} --cleanup"
               }
            }

          }
        }
      }
    }
  }
