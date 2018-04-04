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
        //containerTemplate(name: 'helm', image: 'lachlanevenson/k8s-helm:v2.6.0', command: 'cat', ttyEnabled: true),
        containerTemplate(name: 'maven', image: 'maven:3.3.9-jdk-8-alpine', ttyEnabled: true, command: 'cat')
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

        container('helm'){
          sh 'echo ">>> Running Helm cli Test"'
          sh 'echo "MIIEpAIBAAKCAQEA1F+ng1TSGjIcrIkedhDTw3oWdRQ6Jr16FSvTWM3zuwzTFsp7JjMgEpKGDAtqBimg6LVuPMOFdGhVCxE2xcJ9Ma4Ak14QaB0jiOE3THhQTOpSwA0vSmJODx9zaGCN3b0XMn036Ctd91V1YHGFKpUdEyw+p3XZ20jyL3ec/alYYOo/XFvij4rCKYKrQaF56ttqR7HpEv2Ju5BzfKpdcHvXdl9RpWqjmHbZVA59EnT6Cnb8TqZRlbom65prWnFruU4pezvUtAJvnxySIqHvDMg8f9buHx+BFnyCeZfq+S3QKk9RJ2FWVLVYjZ0xfy4eGihWKM9ryN5LuVMK77haiZG3AwIDAQABAoIBAF/o1wDjpIL6EKMGxb/yN4B3OX8kZGKsfV7kTO01DZZy4z3OsbD9s8VPcMQtv3MLB8Uwcpl0f2ej2oxF+ON0ww9VkqL6/xPV3P9rHoslZrZluHtNOQcxwCjqPjdsK4VxaPF/RWlPdH9Hk9u7SLWDY/8NozoDaiCzH9S6Ayc3fFc/gDkun1S3x32m/yzYRt1nqYO2LdeXNPgD7zfK1Dx3HZD1f2zGHE/NcepTNB4xgfHhqK9l6+Q/DcBFcER2pURwgLIy2W31hhssxp86Llg507ECqkdBEZni/XQK0uBz1HXcSAdhlVbob4I+lDsUceQhjGixPWWHowRGi1Gz6wcDVUECgYEA7hyBHKadSOBPXShRQPVgd20fBLx247UELjTI8AkW7lsjme3qKTp19sd8V1SCsPqk4lgiV7NmiAMR6W9VRcRHOZ//n17kP9QUIbxq2d0eGI/PStG8osnWdHNZbAmwpE8Z4FfenJfvWyTGugtXLWwlD9B7kjfz3I7dzOsoOrTjV6MCgYEA5FQlz9qYK5wGJ/J7U1ny1yVpggDnZbSuVs6dI5y7llI1h6hehOudl0HJO4AxKo9pzPA9R/HRg7zD1q0KrbXwf7fGrFNfAXOYjygutrZqAT7rSEIR0tSdNBCRux3TtefHW+79V/eyJyVwnUVroDDgVa21xTc6gb7sFxn6Jw3BmSECgYEAsJjXcUZpVMl4UyE50jGq0ChQXxTgIFX6ucJQXSaAqVtS9jEsAFPpdZPSNnrpSxU6AN1Y6y6VFr8gI798wPen06dE0RBxvJ0wKS0zGk4SqijOlzEi9KE5urhqU+SD6/j2uhqxcfaFgVWvRgBvMbMJcccwPuvco3IaMoceGRxbmH0CgYEAlry+4cQMZe3xWnoI1PQzD7pRN1Rlb42i8wggUZxtc0X+tPqAu/vY5Dy4HyH4U4KudG+95TtN+EysdZNz006j4Y1wCeBYflrUQt5iSJmQzhW9usxze96FkhPGQePlGthTkuvqMSMDaDidahakgPMDh0zRDcvyQinLL00lCpdYUkECgYAfwEghyd8JRZ4L6XY+HdipFdutjJGyI9ESMVfCpwEjMnZwQnh+JzkJ4Zn0RAFGmxvIkKlOnsGR4qiyUhsrWf0CLTYQK9sKO4EaWfVp538hifKHa4tq+IFcO/KwwaihnAHvUW/6Kw5LL6wlOE1gWWH98znhHTpuPwCxmzCEWokxCQ==" > /key.pem'
          sh 'echo "MIIDNzCCAh+gAwIBAgIUEODWqPigIHRbq81VrHqAZDbaUNMwDQYJKoZIhvcNAQELBQAwHzEdMBsGA1UEAwwUMTI3LjAuMC4xQDE1MTY4MDQyNTAwHhcNMTgwNDA0MTQzOTAwWhcNMTgwNzAzMTQzOTAwWjAQMQ4wDAYDVQQDEwVhZG1pbjCCASIwDQYJKoZIhvcNAQEBBQADggEPADCCAQoCggEBANRfp4NU0hoyHKyJHnYQ08N6FnUUOia9ehUr01jN87sM0xbKeyYzIBKShgwLagYpoOi1bjzDhXRoVQsRNsXCfTGuAJNeEGgdI4jhN0x4UEzqUsANL0piTg8fc2hgjd29FzJ9N+grXfdVdWBxhSqVHRMsPqd12dtI8i93nP2pWGDqP1xb4o+KwimCq0Gheerbakex6RL9ibuQc3yqXXB713ZfUaVqo5h22VQOfRJ0+gp2/E6mUZW6Juuaa1pxa7lOKXs71LQCb58ckiKh7wzIPH/W7h8fgRZ8gnmX6vkt0CpPUSdhVlS1WI2dMX8uHhooVijPa8jeS7lTCu+4WomRtwMCAwEAAaN6MHgwDgYDVR0PAQH/BAQDAgWgMAwGA1UdEwEB/wQCMAAwHQYDVR0OBBYEFD1v2X2/EBtJv3tFmAELdILzOOF9MB8GA1UdIwQYMBaAFF+W1ql9kl4fuDxkxWFEP4/N1ZDJMBgGA1UdEQQRMA+CDW15Y2x1c3Rlci5pY3AwDQYJKoZIhvcNAQELBQADggEBAJt7uDgHqzL4TVNUjm2KiO2nsGH2PXPM10njzHQKthcfdrkGIf6Za1FBOF3cM7cxq5GIbPvv+TqFElkrjonbPkolUeTa/MoyrFVUSl+LBGoRQCvJ0psDDt2Xt4oLzmS2URzBS2tv+AIIJO8CFvbsXMsYAbGghq3zoXitqWoLf9RN/PGy92vb+lxGWdatlU730j5yG0XvldxFeJ6u8A4cpmuGWSnPKqhv4vgBq2qG7JEeKKU3kjQbbGW5sGZelHiVcIxpZMCEcn1xeKtGMfOJblLLdmWi5IqU7YYEoYGoA5/XVUPHgWgdGWzosgVBSkTtHI7ESy/Ya8036aXBZotxy+I=" > /cert.pem'
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
                        echo ">>> Pushing Image to dockerhub"
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
          container('helm'){

            stage('Initalize Helm'){
              sh 'echo ">>> Initializing Helm..."'
              sh 'helm init'
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

              //sh 'echo ">>>>>>>>>>>>>>>>>>>>> " + $TAG'

              if(config.release.dryrun){
                if(config.release.initialInstall){
                  sh """
                    #!/bin/bash
                    echo ">>> Installing new Helm Deployment Dryrun"
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
