const MIDNIGHT_SUFFIX = /(?:^|\s)00:00:00(?:\.000)?$/

function timestamp(value, inclusiveEnd) {
  if (!value) return undefined

  const date = value instanceof Date ? new Date(value.getTime()) : new Date(value)
  if (Number.isNaN(date.getTime())) return undefined

  // 仅选择结束日期时，Element UI 默认返回当天 00:00:00。
  // 统计范围按自然日理解，因此需要包含该日期的全部交易。
  if (inclusiveEnd && typeof value === 'string' && MIDNIGHT_SUFFIX.test(value)) {
    date.setHours(23, 59, 59, 999)
  }
  return date.getTime()
}

function buildDateRangeParams(dateRange) {
  const [start, end] = dateRange || []
  const minCreated = timestamp(start, false)
  const maxCreated = timestamp(end, true)
  const params = {}

  if (minCreated !== undefined) params.minCreated = minCreated
  if (maxCreated !== undefined) params.maxCreated = maxCreated
  return params
}

module.exports = { buildDateRangeParams }
