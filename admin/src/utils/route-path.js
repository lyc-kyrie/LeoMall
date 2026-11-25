// 阶段2D-4a：浏览器端路由 path.resolve（替代 Node path / path-browserify——后者依赖 process.cwd）
// 语义与 posix path.resolve 一致，cwd 恒为 '/'（后台路由全部以根为基）。
export function resolve(...args) {
  const parts = []
  let absolute = false
  for (let i = args.length - 1; i >= 0 && !absolute; i--) {
    const p = args[i]
    if (typeof p !== 'string' || p === '') continue
    parts.unshift(p)
    absolute = p.startsWith('/')
  }
  const out = []
  for (const seg of parts.join('/').split('/')) {
    if (seg === '' || seg === '.') continue
    if (seg === '..') out.pop()
    else out.push(seg)
  }
  return '/' + out.join('/')
}