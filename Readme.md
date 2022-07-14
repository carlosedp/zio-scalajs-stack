# Full Stack Scala 3 + ZIO + Scala.js

An experimental stack built with Scala 3 and composed of a ZIO backend and a Scala.js frontend.

Libraries used:

- Scala 3
- ZIO
- Scala.js
- scalajsdom
- zio-test for the backend
- scalatest for the frontend
- Nodejs
  - jsdom
  - Vite

## Development and Build

1. Install a JDK and mill build tool in your path or use the bundled one
2. Install Node.js and npm
3. Install NPM dependencies with `npm install`
4. To start the development server, run `npm run start`, it will build the Scala.js Javascript files, start the ZIO backend and run Vite dev server. The Scala.js files are not automatically generated and reloaded. For this to work, open another shell and run `./mill -w frontend.fastLinkJS`.

To bundle the application for production, run `npm run build`. The files will be placed on `./dist`.
