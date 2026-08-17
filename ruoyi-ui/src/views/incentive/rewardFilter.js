function filterVisibleRewards(items) {
  return (Array.isArray(items) ? items : [])
    .filter(item => item?.source?.type !== 'action')
}

module.exports = { filterVisibleRewards }
