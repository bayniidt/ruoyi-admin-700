const assert = require('assert')
const { filterVisibleRewards } = require('./rewardFilter')

const rewards = [
  { key: 'transaction', source: { type: 'transaction' }, amount: 1000 },
  { key: 'action', source: { type: 'action' }, amount: 4000 },
  { key: 'unknown', source: null, amount: 2000 }
]

assert.deepStrictEqual(
  filterVisibleRewards(rewards).map(item => item.key),
  ['transaction', 'unknown']
)
assert.strictEqual(rewards.length, 3)
