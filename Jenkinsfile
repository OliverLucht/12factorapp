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

        
        // copy bluemix file to home of jenkins user, so thet he could use the pr plugin
        // add the myclusterip to etc/hosts  
        // login to the cluster, needed due to security for helm commands  
        // get the .pem files via cluster-config, needed for the --tls command
        container('bxpr'){
            withCredentials([[$class: 'UsernamePasswordMultiBinding', credentialsId: 'bx_pr_credentials',
            usernameVariable: 'BXPR_USER', passwordVariable: 'BXPR_PASSWORD']]) {
                sh """
                #!/bin/bash
                echo ">>> Running Helm cli Test"
                echo ${BXPR_USER}
                echo ${BXPR_PASSWORD} 
                cp -a /root/.bluemix /home/jenkins/              
                echo "10.134.214.140 mycluster.icp" >> /etc/hosts                
                bx pr login -u ${BXPR_USER} -p ${BXPR_PASSWORD} -a https://mycluster.icp:8443 -c id-icp-account --skip-ssl-validation               
                bx pr cluster-config mycluster   
                helm init
                helm list --tls
                helm version --tls
                echo ">>> Helm cli Test Done"
                """
            }
        }

        stage ('Test helm Chart'){
            container('bxpr'){
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
                    echo ">>> Upgrading Helm Deployment"   
                    helm upgrade ${config.release.name} ${config.release.chart_dir} --set image.repository=${REPOSITORY},image.tag=${TAG} --tls
                    echo ">>> Upgrade Done!"
                  """
                }
              }


            if(config.release.test){
              stage('Testing Helm Release'){
                sh 'echo ">>> Running Helm Test"'
                sh "helm test ${config.release.name} --cleanup --tls"
               }
            }
          }
        }
      }
    }
  }
