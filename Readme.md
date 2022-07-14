# Full Stack Scala ZIO + Scala.js

An experimental stack composed of a Scala ZIO backend and a Scala.js frontend.

Libraries used (so far):

- ZIO
- Scala.js
- scalajsdom
- zio-test for the backend
- scalatest for the frontend
- NPM
  - jsdom

## Develop

1. Install mill build tool in your path or use the bundled one
2. Install NPM dependencies with `npm install`
3. Generate the javascript for the frontend with `mill frontend.fastLinkJS`

