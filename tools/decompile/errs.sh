#!/bin/bash
# Compact ECJ error list from /tmp/verify.log (after verify.sh): "file :line ## message ## source". Arg: max width.
grep -E "^[0-9]+\. ERROR" -A4 /tmp/verify.log | grep -vE "^\-\-|^\s*\^" | awk '/ERROR in/{f=$0; getline; src=$0; getline; print f " ## " $0 " ## " src}' | sed -E 's#^[0-9]+\. ERROR in /home/user/Mods/spraute_engine-src/src/main/java/org/sprautestudio/spraute_engine/##; s/\(at line ([0-9]+)\)/:\1/' | sed -E 's/\s+/ /g' | cut -c1-${1:-260}
