const assert = require('assert')
const { buildDateRangeParams } = require('./dateRange')

const fullDay = buildDateRangeParams([
  '2026-07-01 00:00:00',
  '2026-07-31 00:00:00'
])
const inclusiveEnd = new Date(fullDay.maxCreated)

assert.strictEqual(inclusiveEnd.getFullYear(), 2026)
assert.strictEqual(inclusiveEnd.getMonth(), 6)
assert.strictEqual(inclusiveEnd.getDate(), 31)
assert.strictEqual(inclusiveEnd.getHours(), 23)
assert.strictEqual(inclusiveEnd.getMinutes(), 59)
assert.strictEqual(inclusiveEnd.getSeconds(), 59)
assert.strictEqual(inclusiveEnd.getMilliseconds(), 999)

const explicitTime = buildDateRangeParams([
  '2026-07-01 00:00:00',
  '2026-07-31 12:30:45'
])

assert.strictEqual(explicitTime.maxCreated, new Date('2026-07-31 12:30:45').getTime())
