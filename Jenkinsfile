pipeline {
    agent any

    options {
        disableConcurrentBuilds()
        skipDefaultCheckout(true)
        timeout(time: 60, unit: 'MINUTES')
    }

    environment {
        TF_IN_AUTOMATION = 'true'
        TF_INPUT = 'false'
        PATH = '/opt/homebrew/bin:/usr/local/bin:/usr/bin:/bin:/usr/sbin:/sbin'
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Validate Terraform') {
            steps {
                sh '''
                    set -eu
                    command -v terraform
                    command -v az
                    command -v ssh
                    command -v tar
                    command -v curl
                '''
                dir('onesub-infrastructure') {
                    sh 'terraform fmt -check'
                    sh 'terraform init -input=false'
                    sh 'terraform validate'
                }
            }
        }

        stage('Plan Azure changes') {
            steps {
                withCredentials([
                    string(credentialsId: 'onesub-azure-client-id', variable: 'ARM_CLIENT_ID'),
                    string(credentialsId: 'onesub-azure-client-secret', variable: 'ARM_CLIENT_SECRET'),
                    string(credentialsId: 'onesub-azure-tenant-id', variable: 'ARM_TENANT_ID'),
                    string(credentialsId: 'onesub-azure-subscription-id', variable: 'ARM_SUBSCRIPTION_ID'),
                    string(credentialsId: 'onesub-admin-cidr', variable: 'TF_VAR_admin_cidr'),
                    string(credentialsId: 'onesub-ssh-public-key', variable: 'TF_VAR_ssh_public_key')
                ]) {
                    dir('onesub-infrastructure') {
                        sh '''
                            set -eu
                            set +x
                            export TF_VAR_subscription_id="$ARM_SUBSCRIPTION_ID"
                            terraform plan -input=false -out=tfplan
                        '''
                    }
                }
            }
        }

        stage('Approve Azure changes') {
            steps {
                input message: 'Review the Terraform plan in the console output. Apply these Azure changes?', ok: 'Apply'
            }
        }

        stage('Apply Azure changes') {
            steps {
                withCredentials([
                    string(credentialsId: 'onesub-azure-client-id', variable: 'ARM_CLIENT_ID'),
                    string(credentialsId: 'onesub-azure-client-secret', variable: 'ARM_CLIENT_SECRET'),
                    string(credentialsId: 'onesub-azure-tenant-id', variable: 'ARM_TENANT_ID'),
                    string(credentialsId: 'onesub-azure-subscription-id', variable: 'ARM_SUBSCRIPTION_ID'),
                    string(credentialsId: 'onesub-admin-cidr', variable: 'TF_VAR_admin_cidr'),
                    string(credentialsId: 'onesub-ssh-public-key', variable: 'TF_VAR_ssh_public_key')
                ]) {
                    dir('onesub-infrastructure') {
                        sh '''
                            set -eu
                            set +x
                            export TF_VAR_subscription_id="$ARM_SUBSCRIPTION_ID"
                            terraform apply -input=false -auto-approve tfplan
                        '''
                    }
                }
            }
        }

        stage('Verify Artifactory VM') {
            steps {
                script {
                    env.ONESUB_RESOURCE_GROUP = sh(
                        script: 'terraform -chdir=onesub-infrastructure output -raw resource_group_name',
                        returnStdout: true
                    ).trim()
                    env.ONESUB_ARTIFACTORY_VM_NAME = sh(
                        script: 'terraform -chdir=onesub-infrastructure output -raw artifactory_vm_name',
                        returnStdout: true
                    ).trim()
                    env.ONESUB_ARTIFACTORY_PUBLIC_IP = sh(
                        script: 'terraform -chdir=onesub-infrastructure output -raw artifactory_public_ip',
                        returnStdout: true
                    ).trim()
                }
                withCredentials([
                    string(credentialsId: 'onesub-azure-client-id', variable: 'ARM_CLIENT_ID'),
                    string(credentialsId: 'onesub-azure-client-secret', variable: 'ARM_CLIENT_SECRET'),
                    string(credentialsId: 'onesub-azure-tenant-id', variable: 'ARM_TENANT_ID'),
                    string(credentialsId: 'onesub-azure-subscription-id', variable: 'ARM_SUBSCRIPTION_ID')
                ]) {
                    sh '''
                        set -eu
                        set +x
                        az login --service-principal \
                            --username "$ARM_CLIENT_ID" \
                            --password "$ARM_CLIENT_SECRET" \
                            --tenant "$ARM_TENANT_ID" \
                            --output none
                        az account set --subscription "$ARM_SUBSCRIPTION_ID"
                        az vm start \
                            --resource-group "$ONESUB_RESOURCE_GROUP" \
                            --name "$ONESUB_ARTIFACTORY_VM_NAME" \
                            --only-show-errors
                    '''
                }
                sshagent(credentials: ['onesub-vm-ssh']) {
                    sh '''
                        set -eu
                        attempt=0
                        ready=0
                        while [ "$attempt" -lt 90 ]; do
                            if ssh -o BatchMode=yes -o ConnectTimeout=10 \
                                -o StrictHostKeyChecking=accept-new \
                                "onesubadmin@$ONESUB_ARTIFACTORY_PUBLIC_IP" \
                                'sudo cloud-init status --wait >/dev/null && curl --fail --silent --show-error --max-time 10 http://10.40.2.4:8082/ui/ >/dev/null'
                            then
                                ready=1
                                break
                            fi
                            attempt=$((attempt + 1))
                            sleep 10
                        done
                        if [ "$ready" -ne 1 ]; then
                            echo "Artifactory did not become ready within 15 minutes." >&2
                            exit 1
                        fi
                    '''
                }
                sh 'terraform -chdir=onesub-infrastructure output -raw artifactory_ssh_tunnel_command'
            }
        }

        stage('Deploy and verify') {
            steps {
                script {
                    env.ONESUB_VM_PUBLIC_IP = sh(
                        script: 'terraform -chdir=onesub-infrastructure output -raw public_ip_address',
                        returnStdout: true
                    ).trim()
                    env.ONESUB_VM_PUBLIC_FQDN = sh(
                        script: 'terraform -chdir=onesub-infrastructure output -raw public_fqdn',
                        returnStdout: true
                    ).trim()
                    env.ONESUB_RESOURCE_GROUP = sh(
                        script: 'terraform -chdir=onesub-infrastructure output -raw resource_group_name',
                        returnStdout: true
                    ).trim()
                    env.ONESUB_VM_NAME = sh(
                        script: 'terraform -chdir=onesub-infrastructure output -raw virtual_machine_name',
                        returnStdout: true
                    ).trim()
                }
                withCredentials([
                    string(credentialsId: 'onesub-azure-client-id', variable: 'ARM_CLIENT_ID'),
                    string(credentialsId: 'onesub-azure-client-secret', variable: 'ARM_CLIENT_SECRET'),
                    string(credentialsId: 'onesub-azure-tenant-id', variable: 'ARM_TENANT_ID'),
                    string(credentialsId: 'onesub-azure-subscription-id', variable: 'ARM_SUBSCRIPTION_ID')
                ]) {
                    sh '''
                        set -eu
                        set +x
                        az login --service-principal \
                            --username "$ARM_CLIENT_ID" \
                            --password "$ARM_CLIENT_SECRET" \
                            --tenant "$ARM_TENANT_ID" \
                            --output none
                        az account set --subscription "$ARM_SUBSCRIPTION_ID"
                        az vm start \
                            --resource-group "$ONESUB_RESOURCE_GROUP" \
                            --name "$ONESUB_VM_NAME" \
                            --only-show-errors
                    '''
                }
                sshagent(credentials: ['onesub-vm-ssh']) {
                    sh '''
                        set -eu
                        mkdir -p "$HOME/.ssh"
                        chmod 700 "$HOME/.ssh"
                        attempt=0
                        ready=0
                        while [ "$attempt" -lt 36 ]; do
                            if ssh -o BatchMode=yes -o StrictHostKeyChecking=accept-new \
                                "onesubadmin@$ONESUB_VM_PUBLIC_IP" \
                                'sudo cloud-init status --wait >/dev/null && sudo docker compose version >/dev/null'
                            then
                                ready=1
                                break
                            fi
                            attempt=$((attempt + 1))
                            sleep 10
                        done
                        if [ "$ready" -ne 1 ]; then
                            echo "VM did not become ready for deployment within six minutes." >&2
                            exit 1
                        fi
                        bash onesub-infrastructure/deploy.sh "$ONESUB_VM_PUBLIC_IP" "$ONESUB_VM_PUBLIC_FQDN"
                        curl --fail --show-error --retry 12 --retry-delay 10 --retry-all-errors \
                            "https://$ONESUB_VM_PUBLIC_FQDN/api/v1/auth/csrf"
                    '''
                }
            }
        }
    }

    post {
        always {
            sh 'rm -f onesub-infrastructure/tfplan'
        }
    }
}
