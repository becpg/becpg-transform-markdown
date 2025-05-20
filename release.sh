#!/bin/bash

SCRIPT_PATH="${BASH_SOURCE[0]}";
if ([ -h "${SCRIPT_PATH}" ]) then
  while([ -h "${SCRIPT_PATH}" ]) do SCRIPT_PATH=`readlink "${SCRIPT_PATH}"`; done
fi
pushd . > /dev/null
cd `dirname ${SCRIPT_PATH}` > /dev/null
SCRIPT_PATH=`pwd`;
popd  > /dev/null

# Run release:prepare and check its exit status
if mvn release:prepare; then

  SCM_TAG=$(grep '^scm.tag=' ./release.properties | awk -F'=' '{print $2}')
  last_digit="${SCM_TAG##*.}"  # Extract the last part after the last dot
  decremented_last_digit=$((last_digit - 1))  # Decrement the last digit

  # Replace the last digit with the decremented value
  PREVIOUS_SCM_TAG="${SCM_TAG%.*}.$decremented_last_digit"
  CHANGES=$(git log --pretty="* %s" $SCM_TAG...$PREVIOUS_SCM_TAG)
  CUR_CHANGE_DATE=`date`

  mvn release:clean
  rm ./release.properties
  
  git push
  git push --tags

  echo "**********************************************************"
  echo "🎁 Release notes : $SCM_TAG"
  echo "**********************************************************"
    
   # Prepare the changelog content
  CHANGELOG_CONTENT=$(printf "## [ $SCM_TAG ] - $CUR_CHANGE_DATE\n\n$CHANGES\n\n\n")

  # Add changes to the Changelog.MD file
  echo -e "$CHANGELOG_CONTENT\n" >> CHANGELOG.md
  
  # Commit and push the updated Changelog.MD file
  git add CHANGELOG.md
  git commit -m "[skip ci][Release]: Update CHANGELOG.md for $SCM_TAG release"
  git push
  
else
  echo "Release failed. use mvn release:rollback command"
fi