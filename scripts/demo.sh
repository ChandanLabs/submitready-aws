#!/usr/bin/env bash
set -euo pipefail
npm test
npm run validate:demo >/tmp/submitready-broken.json
npm run validate:fixed >/tmp/submitready-fixed.json
printf '\nBroken report:\n'
cat /tmp/submitready-broken.json
printf '\nFixed report:\n'
cat /tmp/submitready-fixed.json
