pipeline {
    agent any

    options {
        disableConcurrentBuilds()
        skipDefaultCheckout(true)
        timeout(time: 180, unit: 'MINUTES')
    }

    parameters {
        string(
            name: 'ONESUB_ARTIFACT_VERSION',
            defaultValue: '',
            description: 'Enter the version published by the OneSub CI job.'
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
                    string(credentialsId: 'onesub-ssh-public-key', variable: 'TF_VAR_ssh_public_key'),
                    string(credentialsId: 'onesub-postgres-admin-password', variable: 'ONESUB_POSTGRES_ADMIN_PASSWORD')
                ]) {
                    dir('onesub-infrastructure') {
                        sh '''
                            set -eu
                            set +x
                            export TF_VAR_subscription_id="$ARM_SUBSCRIPTION_ID"
                            export TF_VAR_postgres_admin_password="$ONESUB_POSTGRES_ADMIN_PASSWORD"
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
                    string(credentialsId: 'onesub-azure-subscription-id', variable: 'ARM_SUBSCRIPTION_ID')
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

        stage('Read three-tier deployment outputs') {
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

        stage('Manual DNS and TLS setup') {
            steps {
                input(
                    message: "Set the GoDaddy A record for ${env.ONESUB_DOMAIN} to ${env.ONESUB_FRONTEND_IP}. Complete the manual DNS-01 certificate flow and upload onesub-tls-bundle to Key Vault ${env.ONESUB_FRONTEND_KEY_VAULT}, then continue.",
                    ok: 'TLS uploaded'
                )
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
                        secret_file="$(mktemp "${TMPDIR:-/tmp}/onesub-tls-secret.XXXXXX")"
                        trap 'rm -f "$secret_file"' EXIT
                        az keyvault secret show \
                            --vault-name "$ONESUB_FRONTEND_KEY_VAULT" \
                            --name onesub-tls-bundle \
                            --query value \
                            --only-show-errors \
                            --output tsv > "$secret_file"
                        python3 onesub-infrastructure/scripts/validate-manual-tls-bundle.py \
                            "$secret_file" "$ONESUB_DOMAIN"
                    '''
                }
            }
        }

        stage('Publish application artifacts') {
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
