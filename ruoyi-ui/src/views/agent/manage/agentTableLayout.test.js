const assert = require('assert')
const fs = require('fs')
const path = require('path')

const source = fs.readFileSync(path.join(__dirname, 'index.vue'), 'utf8')
const dataColumnLabels = ['用户名', '昵称', '备注', '分成比例', '创建时间']

dataColumnLabels.forEach(label => {
  const column = source.match(new RegExp(`<el-table-column[^>]*label="${label}"[^>]*>`))
  assert(column, `缺少「${label}」表格列`)
  assert(/\bmin-width="210"/.test(column[0]), `「${label}」列必须使用 min-width 参与剩余宽度分配`)
  assert(!/\swidth="210"/.test(column[0]), `「${label}」列不能使用固定 width，否则宽屏下会留下空白表头`)
})

assert(
  /<el-table-column[^>]*label="操作"[^>]*width="140"[^>]*fixed="right"[^>]*>/.test(source),
  '操作列应保持固定宽度并吸附在表格右侧'
)
