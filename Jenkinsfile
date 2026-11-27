pipeline {
    agent any

    options {
        disableConcurrentBuilds()
        skipDefaultCheckout(true)
        timeout(time: 60, unit: 'MINUTES')
    }

    parameters {
        choice(
            name: 'DEPLOY_TARGET',
            choices: ['poc', 'three-tier'],
            description: 'Deploy the existing POC or the new frontend/backend VMSS and PostgreSQL stack.'
        )
        string(
            name: 'ONESUB_ARTIFACT_VERSION',
            defaultValue: '',
            description: 'For three-tier, enter the version published by the OneSub CI job.'
        )
    }

    environment {
        TF_IN_AUTOMATION = 'true'
        TF_INPUT = 'false'
        JAVA_HOME = '/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home'
        PATH = '/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home/bin:/opt/homebrew/bin:/usr/local/bin:/usr/bin:/bin:/usr/sbin:/sbin'
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Validate three-tier artifact version') {
            when {
                expression { params.DEPLOY_TARGET == 'three-tier' }
            }
            steps {
                script {
                    def artifactVersion = (params.ONESUB_ARTIFACT_VERSION ?: '').trim()
                    if (!(artifactVersion ==~ /[A-Za-z0-9][A-Za-z0-9._-]{0,63}/)) {
                        error('Enter the exact artifact version printed by the OneSub CI job.')
                    }
                    env.ONESUB_ARTIFACT_VERSION = artifactVersion
                    env.ONESUB_RELEASE = artifactVersion
                    env.ONESUB_BACKEND_JAR =
                        "${env.WORKSPACE}/onesub-backend/target/deploy-artifact/onesub-backend-${artifactVersion}.jar"
                }
            }
        }

        stage('Download backend artifact from Azure Artifacts') {
            when {
                expression { params.DEPLOY_TARGET == 'three-tier' }
            }
            steps {
                withCredentials([
                    string(
                        credentialsId: 'onesub-azure-artifacts-read-pat',
                        variable: 'AZURE_ARTIFACTS_PAT'
                    )
                ]) {
                    sh '''
                        set -eu
                        set +x
                        settings_file="$(mktemp "${TMPDIR:-/tmp}/onesub-maven-settings.XXXXXX")"
                        trap 'rm -f "$settings_file"' EXIT
                        python3 onesub-backend/scripts/create-azure-artifacts-settings.py "$settings_file"
                        mkdir -p "$(dirname "$ONESUB_BACKEND_JAR")"
                        rm -f "$ONESUB_BACKEND_JAR"
                        cd onesub-backend
                        ./mvnw -B -s "$settings_file" \
                            org.apache.maven.plugins:maven-dependency-plugin:3.8.1:copy \
                            -Dartifact="com.onesub:onesub-backend:${ONESUB_ARTIFACT_VERSION}:jar" \
                            -DoutputDirectory="$(dirname "$ONESUB_BACKEND_JAR")"
                        test -s "$ONESUB_BACKEND_JAR"
                    '''
                }
            }
        }

        stage('Build and test frontend') {
            when {
                expression { params.DEPLOY_TARGET == 'three-tier' }
            }
            steps {
                sh '''
                    set -eu
                    node --version
                    npm --version
                    cd onesub-frontend
                    npm ci
                    npm run test
                    npm run build
                '''
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
                script {
                    def threeTierAlreadyExists = sh(
                        script: "terraform -chdir=onesub-infrastructure state list | grep -Fq 'module.three_tier[0].'",
                        returnStatus: true
                    ) == 0
                    env.ONESUB_ENABLE_THREE_TIER =
                        params.DEPLOY_TARGET == 'three-tier' || threeTierAlreadyExists ? 'true' : 'false'

                    def planCredentials = [
                        string(credentialsId: 'onesub-azure-client-id', variable: 'ARM_CLIENT_ID'),
                        string(credentialsId: 'onesub-azure-client-secret', variable: 'ARM_CLIENT_SECRET'),
                        string(credentialsId: 'onesub-azure-tenant-id', variable: 'ARM_TENANT_ID'),
                        string(credentialsId: 'onesub-azure-subscription-id', variable: 'ARM_SUBSCRIPTION_ID'),
                        string(credentialsId: 'onesub-admin-cidr', variable: 'TF_VAR_admin_cidr'),
                        string(credentialsId: 'onesub-ssh-public-key', variable: 'TF_VAR_ssh_public_key')
                    ]
                    if (env.ONESUB_ENABLE_THREE_TIER == 'true') {
                        planCredentials.add(
                            string(credentialsId: 'onesub-postgres-admin-password', variable: 'ONESUB_POSTGRES_ADMIN_PASSWORD')
                        )
                    }

                    withCredentials(planCredentials) {
                        dir('onesub-infrastructure') {
                            sh '''
                                set -eu
                                set +x
                                export TF_VAR_subscription_id="$ARM_SUBSCRIPTION_ID"
                                export TF_VAR_enable_three_tier="$ONESUB_ENABLE_THREE_TIER"
                                if [ "$ONESUB_ENABLE_THREE_TIER" = "true" ]; then
                                    export TF_VAR_postgres_admin_password="$ONESUB_POSTGRES_ADMIN_PASSWORD"
                                fi
                                terraform plan -input=false -out=tfplan
                            '''
                        }
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

        stage('Deploy and verify') {
            when {
                expression { params.DEPLOY_TARGET == 'poc' }
            }
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

        stage('Read three-tier deployment outputs') {
            when {
                expression { params.DEPLOY_TARGET == 'three-tier' }
            }
            steps {
                script {
                    env.ONESUB_RESOURCE_GROUP = sh(
                        script: 'terraform -chdir=onesub-infrastructure output -raw resource_group_name',
                        returnStdout: true
                    ).trim()
                    env.ONESUB_FRONTEND_IP = sh(
                        script: 'terraform -chdir=onesub-infrastructure output -raw frontend_public_ip',
                        returnStdout: true
                    ).trim()
                    env.ONESUB_FRONTEND_VMSS = sh(
                        script: 'terraform -chdir=onesub-infrastructure output -raw frontend_vmss_name',
                        returnStdout: true
                    ).trim()
                    env.ONESUB_BACKEND_VMSS = sh(
                        script: 'terraform -chdir=onesub-infrastructure output -raw backend_vmss_name',
                        returnStdout: true
                    ).trim()
                    env.ONESUB_FRONTEND_KEY_VAULT = sh(
                        script: 'terraform -chdir=onesub-infrastructure output -raw frontend_key_vault_name',
                        returnStdout: true
                    ).trim()
                    env.ONESUB_BACKEND_KEY_VAULT = sh(
                        script: 'terraform -chdir=onesub-infrastructure output -raw backend_key_vault_name',
                        returnStdout: true
                    ).trim()
                    env.ONESUB_STORAGE_ACCOUNT = sh(
                        script: 'terraform -chdir=onesub-infrastructure output -raw artifact_storage_account_name',
                        returnStdout: true
                    ).trim()
                    env.ONESUB_POSTGRES_FQDN = sh(
                        script: 'terraform -chdir=onesub-infrastructure output -raw postgres_fqdn',
                        returnStdout: true
                    ).trim()
                    env.ONESUB_DB_USERNAME = sh(
                        script: 'terraform -chdir=onesub-infrastructure output -raw postgres_admin_username',
                        returnStdout: true
                    ).trim()
                    env.ONESUB_DOMAIN = sh(
                        script: 'terraform -chdir=onesub-infrastructure output -raw domain_name',
                        returnStdout: true
                    ).trim()
                    env.ONESUB_DB_URL = "jdbc:postgresql://${env.ONESUB_POSTGRES_FQDN}:5432/onesub?sslmode=require"
                    env.ONESUB_RELEASE = params.ONESUB_ARTIFACT_VERSION.trim()
                    env.ONESUB_STORAGE_CONTAINER = 'onesub-artifacts'
                }
            }
        }

        stage('Publish runtime database secrets') {
            when {
                expression { params.DEPLOY_TARGET == 'three-tier' }
            }
            steps {
                withCredentials([
                    string(credentialsId: 'onesub-azure-client-id', variable: 'ARM_CLIENT_ID'),
                    string(credentialsId: 'onesub-azure-client-secret', variable: 'ARM_CLIENT_SECRET'),
                    string(credentialsId: 'onesub-azure-tenant-id', variable: 'ARM_TENANT_ID'),
                    string(credentialsId: 'onesub-azure-subscription-id', variable: 'ARM_SUBSCRIPTION_ID'),
                    string(credentialsId: 'onesub-postgres-admin-password', variable: 'ONESUB_DB_PASSWORD')
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
                        bash onesub-infrastructure/scripts/publish-runtime-secrets.sh
                    '''
                }
            }
        }

        stage('Run Flyway migrations') {
            when {
                expression { params.DEPLOY_TARGET == 'three-tier' }
            }
            steps {
                withCredentials([
                    string(credentialsId: 'onesub-postgres-admin-password', variable: 'ONESUB_DB_PASSWORD'),
                    string(
                        credentialsId: 'onesub-azure-artifacts-read-pat',
                        variable: 'AZURE_ARTIFACTS_PAT'
                    )
                ]) {
                    sh '''
                        set -eu
                        set +x
                        settings_file="$(mktemp "${TMPDIR:-/tmp}/onesub-maven-settings.XXXXXX")"
                        trap 'rm -f "$settings_file"' EXIT
                        python3 onesub-backend/scripts/create-azure-artifacts-settings.py "$settings_file"
                        export ONESUB_FLYWAY_URL="$ONESUB_DB_URL"
                        export ONESUB_DB_USERNAME="$ONESUB_DB_USERNAME"
                        attempt=1
                        while ! (cd onesub-backend && ./mvnw -B -s "$settings_file" flyway:migrate); do
                            if [ "$attempt" -ge 6 ]; then
                                echo "Flyway migration failed after six attempts." >&2
                                exit 1
                            fi
                            attempt=$((attempt + 1))
                            sleep 15
                        done
                    '''
                }
            }
        }

        stage('Provision TLS certificate') {
            when {
                expression { params.DEPLOY_TARGET == 'three-tier' }
            }
            steps {
                withCredentials([
                    string(credentialsId: 'onesub-azure-client-id', variable: 'ARM_CLIENT_ID'),
                    string(credentialsId: 'onesub-azure-client-secret', variable: 'ARM_CLIENT_SECRET'),
                    string(credentialsId: 'onesub-azure-tenant-id', variable: 'ARM_TENANT_ID'),
                    string(credentialsId: 'onesub-azure-subscription-id', variable: 'ARM_SUBSCRIPTION_ID'),
                    string(credentialsId: 'onesub-godaddy-api-key', variable: 'GODADDY_API_KEY'),
                    string(credentialsId: 'onesub-godaddy-api-secret', variable: 'GODADDY_API_SECRET')
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
                        export ONESUB_DOMAIN="$ONESUB_DOMAIN"
                        export ONESUB_FRONTEND_KEY_VAULT="$ONESUB_FRONTEND_KEY_VAULT"
                        bash onesub-infrastructure/scripts/prepare-tls-secrets.sh
                    '''
                }
            }
        }

        stage('Publish application artifacts') {
            when {
                expression { params.DEPLOY_TARGET == 'three-tier' }
            }
            steps {
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
                        export ONESUB_STORAGE_ACCOUNT="$ONESUB_STORAGE_ACCOUNT"
                        export ONESUB_STORAGE_CONTAINER="$ONESUB_STORAGE_CONTAINER"
                        export ONESUB_RELEASE="$ONESUB_RELEASE"
                        export ONESUB_BACKEND_JAR="$ONESUB_BACKEND_JAR"
                        bash onesub-infrastructure/scripts/publish-artifacts.sh
                    '''
                }
            }
        }

        stage('Deploy and verify three-tier') {
            when {
                expression { params.DEPLOY_TARGET == 'three-tier' }
            }
            steps {
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

                        for vmss in "$ONESUB_FRONTEND_VMSS" "$ONESUB_BACKEND_VMSS"; do
                            if [ "$vmss" = "$ONESUB_FRONTEND_VMSS" ]; then
                                service_name="frontend"
                            else
                                service_name="backend"
                            fi
                            instance_ids=$(az vmss list-instances \
                                --resource-group "$ONESUB_RESOURCE_GROUP" \
                                --name "$vmss" \
                                --query '[].instanceId' \
                                --output tsv)
                            if [ -z "$instance_ids" ]; then
                                echo "The $service_name VMSS is scaled to zero during scheduled downtime; deploy during business hours." >&2
                                exit 1
                            fi
                            while IFS= read -r instance_id; do
                                [ -n "$instance_id" ] || continue
                                az vmss run-command invoke \
                                    --resource-group "$ONESUB_RESOURCE_GROUP" \
                                    --name "$vmss" \
                                    --instance-id "$instance_id" \
                                    --command-id RunShellScript \
                                    --scripts 'cloud-init status --wait >/dev/null && systemctl start onesub-refresh.service' \
                                    --only-show-errors \
                                    --output none
                            done <<EOF
$instance_ids
EOF
                        done

                        curl --fail --show-error --retry 12 --retry-delay 10 --retry-all-errors \
                            --resolve "$ONESUB_DOMAIN:443:$ONESUB_FRONTEND_IP" \
                            "https://$ONESUB_DOMAIN/api/v1/auth/csrf"
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
