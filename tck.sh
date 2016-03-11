#! /bin/bash

OPTION=${1:-NO_OPTION}
OPTION_ARGUMENT=${2:-NO_OPTION_ARGUMENT}

AVAILABLE_PROFILES="java, laravel, express"

if [ "$OPTION" = "NO_OPTION" ] ; then
    echo "usage: tck [<option>]"
    echo "Option"
    echo "    clone <dir>      clones stormpath-framework-tck locally under directory <dir>."
    echo "                     If no <dir>, then current dir."
    echo "    run <profile>    runs actual TCK tests using profile <profile>. Valid profiles are $AVAILABLE_PROFILES"
    echo ""
    exit
fi

case "$OPTION" in
    clone)
        if [ "${OPTION_ARGUMENT}" = "NO_OPTION_ARGUMENT" ] ; then DIR="stormpath-framework-tck"; else DIR="${OPTION_ARGUMENT}"; fi
        echo "Checking out TCK"
        git config --global user.email "evangelists@stormpath.com"
        git config --global user.name "stormpath-sdk-java TCK"
        git clone git@github.com:stormpath/stormpath-framework-tck.git ${DIR}
        cd ${DIR}
        git checkout master
        echo "TCK cloned"
        #Let's persist the directory name in a temp file called tck_clone_directory so the run phase can find where tck was cloned
        echo ${DIR} > $TMPDIR/tck_clone_directory
        ;;
    run)
        PROFILE=$OPTION_ARGUMENT
        #Let's read the name of the directory where tck was cloned
        CLONED_DIR=$(head -n 1 $TMPDIR/tck_clone_directory)
        if [ "$PROFILE" = "NO_OPTION_ARGUMENT" ] ; then echo "Missing profile. Valid profiles are $AVAILABLE_PROFILES"; exit; fi
        echo "Setting TCK properties"
        echo "Using profile: ${PROFILE}"
        cd ${CLONED_DIR}
        echo "Running TCK now!"
        mvn -P$PROFILE clean verify
        EXIT_STATUS="$?"
        if [ "$EXIT_STATUS" -ne 0 ]; then
            echo "TCK found errors!. Exit status was $EXIT_STATUS"
            exit $EXIT_STATUS
        fi
        echo "TCK ran successfully, no errors found!"
        ;;
esac