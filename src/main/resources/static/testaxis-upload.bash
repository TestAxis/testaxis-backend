#!/usr/bin/env bash

# TestAxis Test Report Upload Script
#
# This script is largely based on https://github.com/codecov/codecov-bash, a project with the following license:
# Apache License Version 2.0, January 2004
# https://github.com/codecov/codecov-bash/blob/master/LICENSE

set -e +o pipefail

VERSION="20201019-1"

url="http://testaxis-io.herokuapp.com/reports"
env="$TESTAXIS_ENV"
service=""
token=""
search_in=""
search_coverage_in=""
# shellcheck disable=SC2153
exit_with=1
curlargs=""
curl_s="-s"
files=""
cacert="$TESTAXIS_CA_BUNDLE"

_git_root=$(git rev-parse --show-toplevel 2>/dev/null || hg root 2>/dev/null || echo "$PWD")
git_root="$_git_root"
remote_addr=""
if [ "$git_root" = "$PWD" ]; then
  git_root="."
fi

url_o=""
pr_o=""
build_o=""
commit_o=""
search_in_o=""
tag_o=""
branch_o=""
slug_o=""

commit="$VCS_COMMIT_ID"
branch="$VCS_BRANCH_NAME"
pr="$VCS_PULL_REQUEST"
slug="$VCS_SLUG"
tag="$VCS_TAG"
build_url="$CI_BUILD_URL"
build="$CI_BUILD_ID"
job="$CI_JOB_ID"
build_status="unknown"

proj_root="$git_root"

b="\033[0;36m"
g="\033[0;32m"
r="\033[0;31m"
e="\033[0;90m"
x="\033[0m"

show_help() {
  cat <<EOF

  TestAxis Bash $VERSION: test reports upload tool


    -h          Display this help and exit
    -f FILE     Target file(s) to upload

                 -f "path/to/file"     only upload this file
                                       skips searching unless provided patterns below

                 -f '!*.bar'           ignore all files at pattern *.bar
                 -f '*.foo'            include all files at pattern *.foo
                 Must use single quotes.
                 This is non-exclusive, use -s "*.foo" to match specific paths.

    -s DIR       Directory to search for test reports.

    -c DIR       Directory to search for coverage reports.

    -p STATUS    The status of the build.
                 Either the status supplied by a supported CI runner or one of the following values:
                 success, build_failed, tests_failed, or unknown.
                 The status will always be overridden to tests_failed if a test report with failing
                 tests is uploaded.

    -t TOKEN     Set the private repository token
                 (option) set environment variable TESTAXIS_TOKEN=:uuid

                 -t @/path/to/token_file
                 -t uuid

    -e ENV       Specify environment variables to be included with this build
                 Also accepting environment variables: TESTAXIS_ENV=VAR,VAR2

                 -e VAR,VAR2

    -N           The commit SHA of the parent for which you are uploading test reports. If not present,
                 the parent will be determined using the API of your repository provider.
                 When using the repository provider's API, the parent is determined via finding
                 the closest ancestor to the commit.

    -R root dir  Used when not in git/hg project to identify project root directory
    -Z           Exit with 0 if not successful. Default will exit with 1

    -- Override CI Environment Variables --
       These variables are automatically detected by popular CI providers

    -B branch    Specify the branch name
    -C sha       Specify the commit sha
    -P pr        Specify the pull request number
    -b build     Specify the build number
    -T tag       Specify the git tag

    -- Communication --
    -u URL       Set the target url
                 (option) Set environment variable TESTAXIS_URL=https://my-hosted-testaxis.com/reports
    -r SLUG      owner/repo slug of the project
                 (option) set environment variable TESTAXIS_SLUG=:owner/:repo
    -S PATH      File path to your cacert.pem file used to verify ssl
                 (option) Set environment variable: TESTAXIS_CA_BUNDLE="/path/to/ca.pem"
    -U curlargs  Extra curl arguments to communicate with TestAxis. e.g., -U "--proxy http://http-proxy"

    -- Debugging --
    -K           Remove color from the output
    -v           Verbose mode

EOF
}

say() {
  echo -e "$1"
}

urlencode() {
  echo "$1" | curl -Gso /dev/null -w "%{url_effective}" --data-urlencode @- "" | cut -c 3- | sed -e 's/%0A//'
}

if [ $# != 0 ]; then
  while getopts "a:A:b:B:c:C:dD:e:f:F:g:G:hJ:k:Kn:p:P:q:r:R:s:S:t:T:u:U:vx:X:ZN:" o; do
    case "$o" in
    "N")
      parent=$OPTARG
      ;;
    "b")
      build_o="$OPTARG"
      ;;
    "B")
      branch_o="$OPTARG"
      ;;
    "c")
      search_coverage_in="$OPTARG"
      ;;
    "C")
      commit_o="$OPTARG"
      ;;
    "e")
      env="$env,$OPTARG"
      ;;
    "h")
      show_help
      exit 0
      ;;
    "K")
      b=""
      g=""
      r=""
      e=""
      x=""
      ;;
    "p")
      build_status="$OPTARG"
      ;;
    "P")
      pr_o="$OPTARG"
      ;;
    "r")
      slug_o="$OPTARG"
      ;;
    "R")
      git_root="$OPTARG"
      ;;
    "s")
      if [ "$search_in_o" = "" ]; then
        search_in_o="$OPTARG"
      else
        search_in_o="$search_in_o $OPTARG"
      fi
      ;;
    "S")
      # shellcheck disable=SC2089
      cacert="--cacert \"$OPTARG\""
      ;;
    "t")
      if [ "${OPTARG::1}" = "@" ]; then
        token=$(tr <"${OPTARG:1}" -d ' \n')
      else
        token="$OPTARG"
      fi
      ;;
    "T")
      tag_o="$OPTARG"
      ;;
    "u")
      url_o=$(echo "$OPTARG" | sed -e 's/\/$//')
      ;;
    "U")
      curlargs="$OPTARG"
      ;;
    "v")
      set -x
      curl_s=""
      ;;
    "Z")
      exit_with=0
      ;;
    *)
      echo -e "${r}Unexpected flag not supported${x}"
      ;;
    esac
  done
fi

say "
  _____       _     _        _
 |_   _|__ __| |_  /_\  __ _(_)___
   | |/ -_|_-<  _|/ _ \ \ \ / (_-<
   |_|\___/__/\__/_/ \_\/_\_\_/__/
                       V$VERSION
"

search_in="$proj_root"

#shellcheck disable=SC2154
if [ "$JENKINS_URL" != "" ]; then
  say "$e==>$x Jenkins CI detected."
  # https://wiki.jenkins-ci.org/display/JENKINS/Building+a+software+project
  # https://wiki.jenkins-ci.org/display/JENKINS/GitHub+pull+request+builder+plugin#GitHubpullrequestbuilderplugin-EnvironmentVariables
  service="jenkins"

  # shellcheck disable=SC2154
  if [ "$ghprbSourceBranch" != "" ]; then
    branch="$ghprbSourceBranch"
  elif [ "$GIT_BRANCH" != "" ]; then
    branch="$GIT_BRANCH"
  elif [ "$BRANCH_NAME" != "" ]; then
    branch="$BRANCH_NAME"
  fi

  # shellcheck disable=SC2154
  if [ "$ghprbActualCommit" != "" ]; then
    commit="$ghprbActualCommit"
  elif [ "$GIT_COMMIT" != "" ]; then
    commit="$GIT_COMMIT"
  fi

  # shellcheck disable=SC2154
  if [ "$ghprbPullId" != "" ]; then
    pr="$ghprbPullId"
  elif [ "$CHANGE_ID" != "" ]; then
    pr="$CHANGE_ID"
  fi

  build="$BUILD_NUMBER"
  # shellcheck disable=SC2153
  build_url=$(urlencode "$BUILD_URL")

elif [ "$CI" = "true" ] && [ "$TRAVIS" = "true" ] && [ "$SHIPPABLE" != "true" ]; then
  say "$e==>$x Travis CI detected."
  # https://docs.travis-ci.com/user/environment-variables/
  service="travis"
  commit="${TRAVIS_PULL_REQUEST_SHA:-$TRAVIS_COMMIT}"
  build="$TRAVIS_JOB_NUMBER"
  pr="$TRAVIS_PULL_REQUEST"
  job="$TRAVIS_JOB_ID"
  slug="$TRAVIS_REPO_SLUG"
  env="$env,TRAVIS_OS_NAME"
  tag="$TRAVIS_TAG"
  if [ "$TRAVIS_BRANCH" != "$TRAVIS_TAG" ]; then
    branch="$TRAVIS_BRANCH"
  fi

  language=$(compgen -A variable | grep "^TRAVIS_.*_VERSION$" | head -1)
  if [ "$language" != "" ]; then
    env="$env,${!language}"
  fi

elif [ "$CODEBUILD_CI" = "true" ]; then
  say "$e==>$x AWS Codebuild detected."
  # https://docs.aws.amazon.com/codebuild/latest/userguide/build-env-ref-env-vars.html
  service="codebuild"
  commit="$CODEBUILD_RESOLVED_SOURCE_VERSION"
  build="$CODEBUILD_BUILD_ID"
  branch="$(echo "$CODEBUILD_WEBHOOK_HEAD_REF" | sed 's/^refs\/heads\///')"
  if [ "${CODEBUILD_SOURCE_VERSION/pr/}" = "$CODEBUILD_SOURCE_VERSION" ]; then
    pr="false"
  else
    pr="$(echo "$CODEBUILD_SOURCE_VERSION" | sed 's/^pr\///')"
  fi
  job="$CODEBUILD_BUILD_ID"
  slug="$(echo "$CODEBUILD_SOURCE_REPO_URL" | sed 's/^.*:\/\/[^\/]*\///' | sed 's/\.git$//')"

elif [ "$DOCKER_REPO" != "" ]; then
  say "$e==>$x Docker detected."
  # https://docs.docker.com/docker-cloud/builds/advanced/
  service="docker"
  branch="$SOURCE_BRANCH"
  commit="$SOURCE_COMMIT"
  slug="$DOCKER_REPO"
  tag="$CACHE_TAG"
  env="$env,IMAGE_NAME"

elif [ "$CI" = "true" ] && [ "$CI_NAME" = "codeship" ]; then
  say "$e==>$x Codeship CI detected."
  # https://www.codeship.io/documentation/continuous-integration/set-environment-variables/
  service="codeship"
  branch="$CI_BRANCH"
  build="$CI_BUILD_NUMBER"
  build_url=$(urlencode "$CI_BUILD_URL")
  commit="$CI_COMMIT_ID"

elif [ -n "$CF_BUILD_URL" ] && [ -n "$CF_BUILD_ID" ]; then
  say "$e==>$x Codefresh CI detected."
  # https://docs.codefresh.io/v1.0/docs/variables
  service="codefresh"
  branch="$CF_BRANCH"
  build="$CF_BUILD_ID"
  build_url=$(urlencode "$CF_BUILD_URL")
  commit="$CF_REVISION"

elif [ "$TEAMCITY_VERSION" != "" ]; then
  say "$e==>$x TeamCity CI detected."
  # https://confluence.jetbrains.com/display/TCD8/Predefined+Build+Parameters
  # https://confluence.jetbrains.com/plugins/servlet/mobile#content/view/74847298
  if [ "$TEAMCITY_BUILD_BRANCH" = '' ]; then
    echo "    Teamcity does not automatically make build parameters available as environment variables."
    echo "    Add the following environment parameters to the build configuration"
    echo "    env.TEAMCITY_BUILD_BRANCH = %teamcity.build.branch%"
    echo "    env.TEAMCITY_BUILD_ID = %teamcity.build.id%"
    echo "    env.TEAMCITY_BUILD_URL = %teamcity.serverUrl%/viewLog.html?buildId=%teamcity.build.id%"
    echo "    env.TEAMCITY_BUILD_COMMIT = %system.build.vcs.number%"
    echo "    env.TEAMCITY_BUILD_REPOSITORY = %vcsroot.<YOUR TEAMCITY VCS NAME>.url%"
  fi
  service="teamcity"
  branch="$TEAMCITY_BUILD_BRANCH"
  build="$TEAMCITY_BUILD_ID"
  build_url=$(urlencode "$TEAMCITY_BUILD_URL")
  if [ "$TEAMCITY_BUILD_COMMIT" != "" ]; then
    commit="$TEAMCITY_BUILD_COMMIT"
  else
    commit="$BUILD_VCS_NUMBER"
  fi
  remote_addr="$TEAMCITY_BUILD_REPOSITORY"

elif [ "$CI" = "true" ] && [ "$CIRCLECI" = "true" ]; then
  say "$e==>$x Circle CI detected."
  # https://circleci.com/docs/environment-variables
  service="circleci"
  branch="$CIRCLE_BRANCH"
  build="$CIRCLE_BUILD_NUM"
  job="$CIRCLE_NODE_INDEX"
  if [ "$CIRCLE_PROJECT_REPONAME" != "" ]; then
    slug="$CIRCLE_PROJECT_USERNAME/$CIRCLE_PROJECT_REPONAME"
  else
    # git@github.com:owner/repo.git
    slug="${CIRCLE_REPOSITORY_URL##*:}"
    # owner/repo.git
    slug="${slug%%.git}"
  fi
  pr="$CIRCLE_PR_NUMBER"
  commit="$CIRCLE_SHA1"
  search_in="$search_in $CIRCLE_ARTIFACTS $CIRCLE_TEST_REPORTS"

elif [ "$BUDDYBUILD_BRANCH" != "" ]; then
  say "$e==>$x buddybuild detected"
  # http://docs.buddybuild.com/v6/docs/custom-prebuild-and-postbuild-steps
  service="buddybuild"
  branch="$BUDDYBUILD_BRANCH"
  build="$BUDDYBUILD_BUILD_NUMBER"
  build_url="https://dashboard.buddybuild.com/public/apps/$BUDDYBUILD_APP_ID/build/$BUDDYBUILD_BUILD_ID"
  # BUDDYBUILD_TRIGGERED_BY

elif [ "${bamboo_planRepository_revision}" != "" ]; then
  say "$e==>$x Bamboo detected"
  # https://confluence.atlassian.com/bamboo/bamboo-variables-289277087.html#Bamboovariables-Build-specificvariables
  service="bamboo"
  commit="${bamboo_planRepository_revision}"
  # shellcheck disable=SC2154
  branch="${bamboo_planRepository_branch}"
  # shellcheck disable=SC2154
  build="${bamboo_buildNumber}"
  # shellcheck disable=SC2154
  build_url="${bamboo_buildResultsUrl}"
  # shellcheck disable=SC2154
  remote_addr="${bamboo_planRepository_repositoryUrl}"

elif [ "$CI" = "true" ] && [ "$BITRISE_IO" = "true" ]; then
  # http://devcenter.bitrise.io/faq/available-environment-variables/
  say "$e==>$x Bitrise CI detected."
  service="bitrise"
  branch="$BITRISE_GIT_BRANCH"
  build="$BITRISE_BUILD_NUMBER"
  build_url=$(urlencode "$BITRISE_BUILD_URL")
  pr="$BITRISE_PULL_REQUEST"
  if [ "$GIT_CLONE_COMMIT_HASH" != "" ]; then
    commit="$GIT_CLONE_COMMIT_HASH"
  fi

elif [ "$CI" = "true" ] && [ "$SEMAPHORE" = "true" ]; then
  say "$e==>$x Semaphore CI detected."
  # https://docs.semaphoreci.com/ci-cd-environment/environment-variables/#semaphore-related
  service="semaphore"
  branch="$SEMAPHORE_GIT_BRANCH"
  build="$SEMAPHORE_WORKFLOW_NUMBER"
  job="$SEMAPHORE_JOB_ID"
  pr="$PULL_REQUEST_NUMBER"
  slug="$SEMAPHORE_REPO_SLUG"
  commit="$REVISION"
  env="$env,SEMAPHORE_TRIGGER_SOURCE"

elif [ "$CI" = "true" ] && [ "$BUILDKITE" = "true" ]; then
  say "$e==>$x Buildkite CI detected."
  # https://buildkite.com/docs/guides/environment-variables
  service="buildkite"
  branch="$BUILDKITE_BRANCH"
  build="$BUILDKITE_BUILD_NUMBER"
  job="$BUILDKITE_JOB_ID"
  build_url=$(urlencode "$BUILDKITE_BUILD_URL")
  slug="$BUILDKITE_PROJECT_SLUG"
  commit="$BUILDKITE_COMMIT"
  if [[ "$BUILDKITE_PULL_REQUEST" != "false" ]]; then
    pr="$BUILDKITE_PULL_REQUEST"
  fi
  tag="$BUILDKITE_TAG"

elif [ "$CI" = "drone" ] || [ "$DRONE" = "true" ]; then
  say "$e==>$x Drone CI detected."
  # http://docs.drone.io/env.html
  # drone commits are not full shas
  service="drone.io"
  branch="$DRONE_BRANCH"
  build="$DRONE_BUILD_NUMBER"
  build_url=$(urlencode "${DRONE_BUILD_LINK}")
  pr="$DRONE_PULL_REQUEST"
  job="$DRONE_JOB_NUMBER"
  tag="$DRONE_TAG"

elif [ "$CI" = "true" ] && [ "$HEROKU_TEST_RUN_BRANCH" != "" ]; then
  say "$e==>$x Heroku CI detected."
  # https://devcenter.heroku.com/articles/heroku-ci#environment-variables
  service="heroku"
  branch="$HEROKU_TEST_RUN_BRANCH"
  build="$HEROKU_TEST_RUN_ID"
  commit="$HEROKU_TEST_RUN_COMMIT_VERSION"

elif [[ "$CI" == "true" || "$CI" == "True" ]] && [[ "$APPVEYOR" == "true" || "$APPVEYOR" == "True" ]]; then
  say "$e==>$x Appveyor CI detected."
  # http://www.appveyor.com/docs/environment-variables
  service="appveyor"
  branch="$APPVEYOR_REPO_BRANCH"
  build=$(urlencode "$APPVEYOR_JOB_ID")
  pr="$APPVEYOR_PULL_REQUEST_NUMBER"
  job="$APPVEYOR_ACCOUNT_NAME%2F$APPVEYOR_PROJECT_SLUG%2F$APPVEYOR_BUILD_VERSION"
  slug="$APPVEYOR_REPO_NAME"
  commit="$APPVEYOR_REPO_COMMIT"
  build_url=$(urlencode "${APPVEYOR_URL}/project/${APPVEYOR_REPO_NAME}/builds/$APPVEYOR_BUILD_ID/job/${APPVEYOR_JOB_ID}")

elif [ "$CI" = "true" ] && [ "$WERCKER_GIT_BRANCH" != "" ]; then
  say "$e==>$x Wercker CI detected."
  # http://devcenter.wercker.com/articles/steps/variables.html
  service="wercker"
  branch="$WERCKER_GIT_BRANCH"
  build="$WERCKER_MAIN_PIPELINE_STARTED"
  slug="$WERCKER_GIT_OWNER/$WERCKER_GIT_REPOSITORY"
  commit="$WERCKER_GIT_COMMIT"

elif [ "$CI" = "true" ] && [ "$MAGNUM" = "true" ]; then
  say "$e==>$x Magnum CI detected."
  # https://magnum-ci.com/docs/environment
  service="magnum"
  branch="$CI_BRANCH"
  build="$CI_BUILD_NUMBER"
  commit="$CI_COMMIT"

elif [ "$SHIPPABLE" = "true" ]; then
  say "$e==>$x Shippable CI detected."
  # http://docs.shippable.com/ci_configure/
  service="shippable"
  # shellcheck disable=SC2153
  branch=$([ "$HEAD_BRANCH" != "" ] && echo "$HEAD_BRANCH" || echo "$BRANCH")
  build="$BUILD_NUMBER"
  build_url=$(urlencode "$BUILD_URL")
  pr="$PULL_REQUEST"
  slug="$REPO_FULL_NAME"
  # shellcheck disable=SC2153
  commit="$COMMIT"

elif [ "$TDDIUM" = "true" ]; then
  say "Solano CI detected."
  # http://docs.solanolabs.com/Setup/tddium-set-environment-variables/
  service="solano"
  commit="$TDDIUM_CURRENT_COMMIT"
  branch="$TDDIUM_CURRENT_BRANCH"
  build="$TDDIUM_TID"
  pr="$TDDIUM_PR_ID"

elif [ "$GREENHOUSE" = "true" ]; then
  say "$e==>$x Greenhouse CI detected."
  # http://docs.greenhouseci.com/docs/environment-variables-files
  service="greenhouse"
  branch="$GREENHOUSE_BRANCH"
  build="$GREENHOUSE_BUILD_NUMBER"
  build_url=$(urlencode "$GREENHOUSE_BUILD_URL")
  pr="$GREENHOUSE_PULL_REQUEST"
  commit="$GREENHOUSE_COMMIT"
  search_in="$search_in $GREENHOUSE_EXPORT_DIR"

elif [ "$GITLAB_CI" != "" ]; then
  say "$e==>$x GitLab CI detected."
  # http://doc.gitlab.com/ce/ci/variables/README.html
  service="gitlab"
  branch="${CI_BUILD_REF_NAME:-$CI_COMMIT_REF_NAME}"
  build="${CI_BUILD_ID:-$CI_JOB_ID}"
  remote_addr="${CI_BUILD_REPO:-$CI_REPOSITORY_URL}"
  commit="${CI_BUILD_REF:-$CI_COMMIT_SHA}"
  slug="${CI_PROJECT_PATH}"

elif [ "$GITHUB_ACTIONS" != "" ]; then
  say "$e==>$x GitHub Actions detected."

  # https://github.com/features/actions
  service="github-actions"

  # https://help.github.com/en/articles/virtual-environments-for-github-actions#environment-variables
  branch="${GITHUB_REF#refs/heads/}"
  if [ "$GITHUB_HEAD_REF" != "" ]; then
    # PR refs are in the format: refs/pull/7/merge
    pr="${GITHUB_REF#refs/pull/}"
    pr="${pr%/merge}"
    branch="${GITHUB_HEAD_REF}"
  fi
  commit="${GITHUB_SHA}"
  slug="${GITHUB_REPOSITORY}"
  build="${GITHUB_RUN_ID}"
  build_url=$(urlencode "http://github.com/${GITHUB_REPOSITORY}/actions/runs/${GITHUB_RUN_ID}")

  case "$build_status" in
    "success") build_status="success" ;;
    "failure") build_status="build_failed" ;;
    "cancelled") build_status="build_failed" ;;
    *) build_status="unknown" ;;
  esac

elif [ "$SYSTEM_TEAMFOUNDATIONSERVERURI" != "" ]; then
  say "$e==>$x Azure Pipelines detected."
  # https://docs.microsoft.com/en-us/azure/devops/pipelines/build/variables?view=vsts
  # https://docs.microsoft.com/en-us/azure/devops/pipelines/build/variables?view=azure-devops&viewFallbackFrom=vsts&tabs=yaml
  service="azure_pipelines"
  commit="$BUILD_SOURCEVERSION"
  build="$BUILD_BUILDNUMBER"
  if [ -z "$SYSTEM_PULLREQUEST_PULLREQUESTNUMBER" ]; then
    pr="$SYSTEM_PULLREQUEST_PULLREQUESTID"
  else
    pr="$SYSTEM_PULLREQUEST_PULLREQUESTNUMBER"
  fi
  project="${SYSTEM_TEAMPROJECT}"
  server_uri="${SYSTEM_TEAMFOUNDATIONSERVERURI}"
  job="${BUILD_BUILDID}"
  branch="$BUILD_SOURCEBRANCHNAME"
  build_url=$(urlencode "${SYSTEM_TEAMFOUNDATIONSERVERURI}${SYSTEM_TEAMPROJECT}/_build/results?buildId=${BUILD_BUILDID}")
elif [ "$CI" = "true" ] && [ "$BITBUCKET_BUILD_NUMBER" != "" ]; then
  say "$e==>$x Bitbucket detected."
  # https://confluence.atlassian.com/bitbucket/variables-in-pipelines-794502608.html
  service="bitbucket"
  branch="$BITBUCKET_BRANCH"
  build="$BITBUCKET_BUILD_NUMBER"
  slug="$BITBUCKET_REPO_OWNER/$BITBUCKET_REPO_SLUG"
  job="$BITBUCKET_BUILD_NUMBER"
  pr="$BITBUCKET_PR_ID"
  commit="$BITBUCKET_COMMIT"
  # See https://jira.atlassian.com/browse/BCLOUD-19393
  if [ "${#commit}" = 12 ]; then
    commit=$(git rev-parse "$BITBUCKET_COMMIT")
  fi
elif [ "$CI" = "true" ] && [ "$BUDDY" = "true" ]; then
  say "$e==>$x Buddy CI detected."
  # https://buddy.works/docs/pipelines/environment-variables
  service="buddy"
  branch="$BUDDY_EXECUTION_BRANCH"
  build="$BUDDY_EXECUTION_ID"
  build_url=$(urlencode "$BUDDY_EXECUTION_URL")
  commit="$BUDDY_EXECUTION_REVISION"
  pr="$BUDDY_EXECUTION_PULL_REQUEST_NO"
  tag="$BUDDY_EXECUTION_TAG"
  slug="$BUDDY_REPO_SLUG"

elif [ "$CIRRUS_CI" != "" ]; then
  say "$e==>$x Cirrus CI detected."
  # https://cirrus-ci.org/guide/writing-tasks/#environment-variables
  service="cirrus-ci"
  slug="$CIRRUS_REPO_FULL_NAME"
  branch="$CIRRUS_BRANCH"
  pr="$CIRRUS_PR"
  commit="$CIRRUS_CHANGE_IN_REPO"
  build="$CIRRUS_TASK_ID"
  job="$CIRRUS_TASK_NAME"

else
  say "${r}x>${x} No CI provider detected."

fi

say "    ${e}project root:${x} $git_root"

# find branch, commit, repo from git command
if [ "$GIT_BRANCH" != "" ]; then
  branch="$GIT_BRANCH"

elif [ "$branch" = "" ]; then
  branch=$(git rev-parse --abbrev-ref HEAD 2>/dev/null || hg branch 2>/dev/null || echo "")
  if [ "$branch" = "HEAD" ]; then
    branch=""
  fi
fi

if [ "$commit_o" = "" ]; then
  # merge commit -> actual commit
  mc=
  if [ -n "$pr" ] && [ "$pr" != false ]; then
    mc=$(git show --no-patch --format="%P" 2>/dev/null || echo "")
  fi
  if [[ "$mc" =~ ^[a-z0-9]{40}[[:space:]][a-z0-9]{40}$ ]]; then
    say "    Fixing merge commit SHA"
    commit=$(echo "$mc" | cut -d' ' -f2)
  elif [ "$GIT_COMMIT" != "" ]; then
    commit="$GIT_COMMIT"
  elif [ "$commit" = "" ]; then
    commit=$(git log -1 --format="%H" 2>/dev/null || hg id -i --debug 2>/dev/null | tr -d '+' || echo "")
  fi
else
  commit="$commit_o"
fi

if [ "$TESTAXIS_TOKEN" != "" ] && [ "$token" = "" ]; then
  say "${e}-->${x} token set from env"
  token="$TESTAXIS_TOKEN"
fi

if [ "$TESTAXIS_URL" != "" ] && [ "$url_o" = "" ]; then
  say "${e}-->${x} url set from env"
  url_o=$(echo "$TESTAXIS_URL" | sed -e 's/\/$//')
fi

if [ "$TESTAXIS_SLUG" != "" ]; then
  say "${e}-->${x} slug set from env"
  slug_o="$TESTAXIS_SLUG"

elif [ "$slug" = "" ]; then
  if [ "$remote_addr" = "" ]; then
    remote_addr=$(git config --get remote.origin.url || hg paths default || echo '')
  fi
  if [ "$remote_addr" != "" ]; then
    if echo "$remote_addr" | grep -q "//"; then
      # https
      slug=$(echo "$remote_addr" | cut -d / -f 4,5 | sed -e 's/\.git$//')
    else
      # ssh
      slug=$(echo "$remote_addr" | cut -d : -f 2 | sed -e 's/\.git$//')
    fi
  fi
  if [ "$slug" = "/" ]; then
    slug=""
  fi
fi

if [ "$branch_o" != "" ]; then
  branch=$(urlencode "$branch_o")
else
  branch=$(urlencode "$branch")
fi

if [ "$slug_o" = "" ]; then
  urlencoded_slug=$(urlencode "$slug")
else
  urlencoded_slug=$(urlencode "$slug_o")
fi

query="branch=$branch       &commit=$commit       &build=$([ "$build_o" = "" ] && echo "$build" || echo "$build_o")\
       &build_url=$build_url       &tag=$([ "$tag_o" = "" ] && echo "$tag" || echo "$tag_o")\
       &slug=$urlencoded_slug       &service=$service       &pr=$([ "$pr_o" = "" ] && echo "${pr##\#}" || echo "${pr_o##\#}")\
       &job=$job       &build_status=$build_status"

if [ -n "$project" ] && [ -n "$server_uri" ]; then
  query=$(echo "$query&project=$project&server_uri=$server_uri" | tr -d ' ')
fi

if [ "$parent" != "" ]; then
  query=$(echo "parent=$parent&$query" | tr -d ' ')
fi

if [ "$url_o" != "" ]; then
  url="$url_o"
fi

query=$(echo "${query}" | tr -d ' ')
# Full query without token (to display on terminal output)
queryNoToken=$(echo "package=bash-$VERSION&token=secret&$query" | tr -d ' ')
# now add token to query
query=$(echo "package=bash-$VERSION&token=$token&$query" | tr -d ' ')

if [ "$search_in_o" != "" ]; then
  # location override
  search_in="${search_in_o%/}"
fi

say "$e==>$x Searching for test reports in:"
for _path in $search_in; do
  say "    ${g}+${x} $_path"
done

patterns="find $search_in -name *.xml"
files=$(eval "$patterns" || echo '')

num_of_files=$(echo "$files" | wc -l | tr -d ' ')
if [ "$num_of_files" != '' ] && [ "$files" != '' ]; then
  say "    ${e}->${x} Found $num_of_files reports"

  while IFS='' read -r file; do
    replace=$search_in"/"
    say "    ${g}+${x} ${file/$replace/}"
  done <<<"$(echo -e "$files")"
fi

# no files found
if [ "$files" = "" ]; then
  say "${r}-->${x} No test report found."
fi

say "${e}==>${x} Preparing upload"

say "    ${e}url:${x} $url"
say "    ${e}query:${x} $query"

say "${e}==>${x} Uploading test report to TestAxis"

# Construct file upload arguments
curl_files=""
if [ "$files" != "" ]; then
  # shellcheck disable=SC2089
  while IFS='' read -r file; do
    curl_files="$curl_files -F files=@$file"
  done <<<"$(echo -e "$files")"
fi

# shellcheck disable=SC2086,2090
res=$(curl -X POST $curl_s $curlargs $cacert \
  --retry 5 --retry-delay 2 --connect-timeout 2 \
  $curl_files \
  --write-out "\nHTTP %{http_code}" \
  -o - \
  "$url?$query&attempt=$i" || echo 'HTTP 999')

status=$(echo "$res" | tail -1 | cut -d' ' -f2)
if [ "$status" = "" ] || [ "$status" = "200" ]; then
  say "    Upload successful: \n ${b}$(echo "$res" | sed '$d')${x}"
  testaxis_build_id="$(echo "$res" | tail -n2 | head -n1)"
else
  say "    An error occurred: \n ${r}${res}${x}"
  exit ${exit_with}
fi


if [ "$search_coverage_in" = "" ]; then
  say "${r}-->${x} No coverage path given (option -k), ignoring coverage."
  exit 0
fi

say "$e==>$x Searching for coverage reports in:"
for _path in $search_coverage_in; do
  say "    ${g}+${x} $_path"
done

patterns="find $search_coverage_in -name *.xml"
files=$(eval "$patterns" || echo '')

num_of_files=$(echo "$files" | wc -l | tr -d ' ')
if [ "$num_of_files" != '' ] && [ "$files" != '' ]; then
  say "    ${e}->${x} Found $num_of_files reports"

  while IFS='' read -r file; do
    replace=$search_coverage_in"/"
    say "    ${g}+${x} ${file/$replace/}"
  done <<<"$(echo -e "$files")"
fi

# no files found
if [ "$files" = "" ]; then
  say "${r}-->${x} No coverage reports found."
else
  say "${e}==>${x} Preparing upload"

  url="$url/$testaxis_build_id/coverage"

  say "    ${e}url:${x} $url"
  say "    ${e}query:${x} $query"

  say "${e}==>${x} Uploading coverage reports to TestAxis"

  # Construct file upload arguments
  curl_files=""
  if [ "$files" != "" ]; then
    # shellcheck disable=SC2089
    while IFS='' read -r file; do
      curl_files="$curl_files -F files=@$file"
    done <<<"$(echo -e "$files")"
  fi

  # shellcheck disable=SC2086,2090
  res=$(curl -X POST $curl_s $curlargs $cacert \
    --retry 5 --retry-delay 2 --connect-timeout 2 \
    $curl_files \
    --write-out "\nHTTP %{http_code}" \
    -o - \
    "$url?$query&attempt=$i" || echo 'HTTP 999')

  status=$(echo "$res" | tail -1 | cut -d' ' -f2)
  if [ "$status" = "" ] || [ "$status" = "200" ]; then
    say "    Upload successful: \n ${b}$(echo "$res" | sed '$d')${x}"
    exit 0
  else
    say "    An error occurred: \n ${r}${res}${x}"
    exit ${exit_with}
  fi
fi
