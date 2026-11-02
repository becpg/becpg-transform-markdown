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
# Batch mode : release:prepare demande la version, le tag et la version suivante,
# ce qui bloque le script quand il tourne sans operateur.
if mvn -B release:prepare; then

  SCM_TAG=$(grep '^scm.tag=' ./release.properties | awk -F'=' '{print $2}')
  last_digit="${SCM_TAG##*.}"  # Extract the last part after the last dot

  # Previous tag: ask git rather than decrementing the last digit. Decrementing
  # breaks on the first release of a line (2.0.0 -> 2.0.-1, which does not
  # exist) and git log then fails, leaving the release notes empty.
  PREVIOUS_SCM_TAG=$(git describe --tags --abbrev=0 "$SCM_TAG^" 2>/dev/null)
  
    # Increment the current tag for the next version
  next_digit=$((last_digit + 1))
  NEXT_SCM_TAG="${SCM_TAG%.*}.$next_digit"
  
  if [ -n "$PREVIOUS_SCM_TAG" ]; then
    CHANGES=$(git log --pretty="* %s" $PREVIOUS_SCM_TAG...$SCM_TAG)
  else
    echo "No previous tag found, listing the whole history"
    CHANGES=$(git log --pretty="* %s" $SCM_TAG)
  fi
  CUR_CHANGE_DATE=`date`

  mvn release:clean
  
  git push
  git push --tags

  echo "**********************************************************"
  echo "🎁 Release notes : $SCM_TAG"
  echo "**********************************************************"
    
   # Prepare the changelog content
  CHANGELOG_CONTENT=$(printf "## [ $SCM_TAG ] - $CUR_CHANGE_DATE\n\n$CHANGES\n\n\n")

  # Add changes to the Changelog.MD file
  echo -e "$CHANGELOG_CONTENT\n" >> CHANGELOG.md
     
  # Update cloudbuild.yml with the next version
  sed -i "s/_VERSION: $SCM_TAG/_VERSION: $NEXT_SCM_TAG/" cloudbuild.delivery.yaml
  
  # Commit and push the updated Changelog.MD file
  git add cloudbuild.delivery.yaml
  git add CHANGELOG.md
  git commit -m "[skip ci][Release]: Update CHANGELOG.md for $SCM_TAG release"
  git push
  
else
  echo "Release failed. use mvn release:rollback command"
fi