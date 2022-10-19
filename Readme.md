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


## Build

The build process supports the following targets:

**Backend:**

- Generate Native Image (GraalVM) binary for Linux in Docker: `mill backend.nativeImage`
- Generate Native Image (GraalVM) binary for current platform (Eg. Mac): `LOCAL_NATIVEIMAGE=1 mill backend.nativeImage`
- Generate Docker Image with Native Image (for Linux in Docker): `mill backend.dockerNative.build`
- Generate Docker Image with a JVM base and .jar app: `mill backend.docker.build`

**Frontend:**

- Build application for deployment (install nodejs, npm and `npm i` first): `npm run build`

## Development

1. Install a JDK in your path
2. Install Node.js and npm
3. Install NPM dependencies with `npm install`
4. To start the development server, run `npm run start`, it will build the Scala.js Javascript files, start the ZIO backend and run Vite dev server. The Scala.js files are not automatically generated and reloaded. For this to work, open another shell and run `./mill -w frontend.fastLinkJS`.

Open <http://localhost:5173> or <http://localhost:5173/?name=Yourname> for testing.
