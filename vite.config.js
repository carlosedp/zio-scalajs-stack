import { spawnSync } from "child_process";
import { defineConfig } from "vite";

// Config project name and web files directory
var projectName = "frontend"
var projectWebFiles = "web"

function isDev() {
    return process.env.NODE_ENV !== "production";
}

function printMillTask(task) {
    const args = ["-s", "--no-server", "--disable-ticker", `${projectName}.${task}`];

    const options = {
        stdio: [
            "pipe", // StdIn.
            "pipe", // StdOut.
            "inherit", // StdErr.
        ],
    };
    const result = process.platform === 'win32'
        ? spawnSync("./mill.bat", args.map(x => `"${x}"`), { shell: true, ...options })
        : spawnSync("./mill", args, options);

    if (result.error)
        throw result.error;
    if (result.status !== 0)
        throw new Error(`mill process failed with exit code ${result.status}`);
    return result.stdout.toString('utf8').trim().split('\n').slice(-1)[0];
}

const linkOutputDir = isDev()
    ? printMillTask("fastLinkOut")
    : printMillTask("fullLinkOut");

export default defineConfig({
    root: `${projectName}/${projectWebFiles}`,
    resolve: {
        alias: [
            {
                find: "@linkOutputDir",
                replacement: linkOutputDir,
            },
        ],
    },
    build: {
        outDir: "../../dist",
        rollupOptions: {
            // https://rollupjs.org/guide/en/#big-list-of-options
            output: {
                dir: "./dist",
            },
        }
    }
});
