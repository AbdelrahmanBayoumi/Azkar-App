const assets = new Bun.Glob("**/*").scanSync("build");

const server = Bun.serve({
  hostname: "127.0.0.1",
  port: Number(Bun.env.PORT ?? 3000),
  routes: Object.fromEntries(
    Array.from(assets, (path) => [
      `/${path}`,
      new Response(Bun.file(`build/${path}`)),
    ]),
  ),
});

console.log(new URL("download.html", server.url).href);
