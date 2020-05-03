scmVars = [:]
activeStageName = ""
dockerImages = []

pipeline {
    agent any

    options {
        skipDefaultCheckout()
    }

    environment {
        GCP_PROJECT_ID = sh(script: "gcloud config get-value project", returnStdout: true).trim()
        DOCKER_REPOSITORY = "us.gcr.io"
        DOCKER_BUILDKIT = "1"
        SLACK_CHANNEL = "dev-hardware-bots"
    }

    stages {
        stage("Checkout") {
            steps {
                script {
                    activeStageName = env.STAGE_NAME

                    scmVars = checkout([$class: 'GitSCM',
                                        branches: scm.branches,
                                        doGenerateSubmoduleConfigurations: false,
                                        extensions: scm.extensions + [[$class: 'SubmoduleOption', parentCredentials: true]],
                                        userRemoteConfigs: scm.userRemoteConfigs])

                    scmVars.GIT_URL = scmVars.GIT_URL.replaceFirst(/\.git$/, "")
                    scmVars.GIT_REPOSITORY = scmVars.GIT_URL.replaceFirst(/^[a-z]+:\/\/[^\/]*\//, "")
                    scmVars.GIT_AUTHOR_NAME = sh(script: "${GIT_EXEC_PATH}/git log -1 --pretty=%an ${scmVars.GIT_COMMIT}", returnStdout: true).trim()
                    scmVars.GIT_AUTHOR_EMAIL = sh(script: "${GIT_EXEC_PATH}/git log -1 --pretty=%ae ${scmVars.GIT_COMMIT}", returnStdout: true).trim()
                    scmVars.GIT_MESSAGE = sh(script: "${GIT_EXEC_PATH}/git log -1 --pretty=%s ${scmVars.GIT_COMMIT}", returnStdout: true).trim()

                    scmVars.each { k, v ->
                         env."${k}" = "${v}"
                    }
                }
            }
        }

        stage("Build") {
            steps {
                script {
                    activeStageName = env.STAGE_NAME
		    
                    docker.image("ubuntu:18.04").inside("-u 0:0 --volume ${env.WORKSPACE}:/workspace") {
                        sh('''#!/bin/bash -xe
                           apt-get update
                           apt-get install -y curl gnupg
                           echo "deb https://dl.bintray.com/sbt/debian /" | tee -a /etc/apt/sources.list.d/sbt.list
                           curl -sL "https://keyserver.ubuntu.com/pks/lookup?op=get&search=0x2EE0EA64E40A89B84B2DF73499E82A75642AC823" | apt-key add
                           apt-get update
                           apt-get install -y openjdk-8-jdk sbt nodejs

                           cd /workspace
                           umask 0

                           if [ -n "${TAG_NAME}" ] ; then
                               sed -i"" "s/^[[:space:]]*version[[:space:]]*:=.*/version := \\"${TAG_NAME}\\"/" build.sbt
                           fi

                           sbt dist
                           ''')
                    }
                }
            }
        }

        stage("Release") {
            when {
                tag pattern: "^v\\d+\\.\\d+\\.\\d+.*\$",
                comparator: "REGEXP"
            }
            steps {
                script {
                    activeStageName = env.STAGE_NAME

                    withCredentials(bindings: [usernamePassword(credentialsId: 'github-recogni-builder',
                                                                usernameVariable: 'GITHUB_CLIENT_ID',
                                                                passwordVariable: 'GITHUB_CLIENT_SECRET')]) {
                    docker.image("ubuntu:18.04").inside("-u 0:0 --volume ${env.WORKSPACE}:/workspace") {
                        sh('''#!/bin/bash -xe
                            apt-get update
                            apt-get install -y --no-install-recommends python3-pip python3-pkg-resources jq
                            pip3 install httpie
                            cd /workspace
                            release_id=$(http --ignore-stdin -a ${GITHUB_CLIENT_ID}:${GITHUB_CLIENT_SECRET} "https://api.github.com/repos/${GIT_REPOSITORY}/releases" | jq -r "map(select(.tag_name == \\"${TAG_NAME}\\")) | .[] | .id")
                            [ ! -z "${release_id}" ]
                            http -a ${GITHUB_CLIENT_ID}:${GITHUB_CLIENT_SECRET} "https://uploads.github.com/repos/${GIT_REPOSITORY}/releases/${release_id}/assets?name=ordt-viewer-${TAG_NAME}.zip" Content-Type:application/octet-stream Expect:100-continue <target/universal/ordt-viewer-*.zip
                            ''')
                        }
                    }
                }
            }
        }
    }

    post {
        always {
            script {
                dockerImages.each() { dockerImage ->
                    sh("docker image rm -f ${dockerImage.id} \$(docker image ls --format '{{.ID}}' ${dockerImage.id}) || true")
                }

                dockerImages = []

                sh("""#!/bin/bash -xe
                   docker container prune -f --filter 'until=24h'
                   docker image prune -f
                   """)
            }
        }

        success {
            sendSlackMessage("Success", "good")
        }

        failure {
            sendSlackMessage("Failure (${activeStageName})", "danger")
        }
    }
}

void sendSlackMessage(String result = "Success", String color = "good", String attachment = "") {
    def author = scmVars.GIT_AUTHOR_NAME.split().first().toLowerCase()
    def message = "${result}: ${author.capitalize()}'s build <${currentBuild.absoluteUrl}|${currentBuild.displayName}> in <${scmVars.GIT_URL}|${scmVars.GIT_REPOSITORY}> (<${scmVars.GIT_URL}/commit/${scmVars.GIT_COMMIT}|${scmVars.GIT_COMMIT.substring(0,8)}> on <${scmVars.GIT_URL}/tree/${scmVars.GIT_BRANCH}|${scmVars.GIT_BRANCH}>)\n• ${scmVars.GIT_MESSAGE}"

    dockerImages.collect() { dockerImage ->
        dockerImage.imageName().split(":").first()
    }.toSet().findAll() { imageName ->
        imageName.startsWith(env.DOCKER_REPOSITORY)
    }.each() { imageName ->
        message += "\nPushed: <https://console.cloud.google.com/gcr/images/${env.GCP_PROJECT_ID}/${env.DOCKER_REPOSITORY.replaceFirst(/\.gcr\.io$/, "")}/${imageName.split("/")[-1]}|${imageName}:*>"
    }

    if (attachment.trim()) {
        message += "\n```" + attachment.trim() + "```"
    }

    def channels = [env.SLACK_CHANNEL]
    def userName = getMatchingSlackUsername(scmVars.GIT_AUTHOR_EMAIL)

    if (userName != "") {
        def channel = "@" + userName
        if (scmVars.GIT_BRANCH =~ /\b(wip)\b/) {
            channels[0] = channel
        }
        else if (result != "Success") {
            channels << channel
        }
    }

    channels.each() { channel ->
        slackSend(channel: channel,
                  color: color,
                  message: message)
    }
}

import static org.apache.commons.lang3.StringUtils.getLevenshteinDistance

@NonCPS
String getMatchingSlackUsername(String author) {
    // If the author string has a recogni.com e-mail address,
    // reduce it to the username portion of the e-mail address
    def pattern = ~/\b(\S+)@recogni\.com\b/
    def matcher = pattern.matcher(author)
    if (matcher.find()) {
        author = matcher.group(1)
    }

    // If the author string has any e-mail address, reduce it
    // to the username portion of the e-mail address with leading digits
    pattern = ~/(?i)\b([A-Z0-9._%+-]+)@[A-Z0-9.-]+(?:\.[A-Z]{2,})?\b/
    matcher = pattern.matcher(author)
    if (matcher.find()) {
        author = matcher.group(1)
        pattern = ~/(?i)^(?:\d+\+)?([^.]+)/
        matcher = pattern.matcher(author)
        if (matcher.find()) {
            author = matcher.group(1)
        }
    }

    // Strip common words from the remaining author string
    pattern = ~/(?i)(?:-|at)(?:recogni|github)/
    author = author.replaceAll(pattern, { "" }).toLowerCase()

    // Destroy the matcher for NonCPS method safety
    matcher = null

    // Find the closest match from the list of Slack usernames
    def userName = (
        ["arad", "arun", "berend", "brudley", "casey", "clau", "dragan", "eugene", "ggoldman", "gilles", "jhhuang", "jigar", "jmb", "johannes", "kalyana", "lukas", "lukasr", "martin", "michael", "michelle", "miroslav", "pradeepj", "shaba", "tihomir", "yesser"].sort { a, b ->
            if (author == a) return -1
            else if (author == b) return 1
            else if (author.startsWith(a) || a.startsWith(author)) return -1
            else if (author.startsWith(b) || b.startsWith(author)) return 1
            else return getLevenshteinDistance(author, a) <=> getLevenshteinDistance(author, b)
        }.first()
    )

    // If the username appears to be close enough, return it
    if (author.startsWith(userName) ||
        userName.startsWith(author) ||
        getLevenshteinDistance(author, userName) < 5) {
        return userName
    }

    // Return empty string if no username match was close enough
    return ""
}
