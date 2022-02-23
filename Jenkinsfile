#!groovy
@Library('uvms-jenkins@main') _
uvmsJenkinsfile {
  MAVEN_PROFILES          = '-Pdocker,jacoco,postgres,publish-sql'
  MAVEN_PROFILES_RELEASE  = '-Ppostgres,publish-sql'
  DOCKER                  = true
}