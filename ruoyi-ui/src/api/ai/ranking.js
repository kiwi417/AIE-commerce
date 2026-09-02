import request from '@/utils/request'

// 热销排行榜
export function listHotRanking(query)   { return request({ url: '/business/ranking/hot/list', method: 'get', params: query }) }
export function saveHotRanking(data)    { return request({ url: '/business/ranking/hot/save', method: 'post', data: data }) }
export function delHotRanking(ids)      { return request({ url: '/business/ranking/hot/' + ids, method: 'delete' }) }

// 运营推荐榜
export function listRecommend(query)    { return request({ url: '/business/ranking/recommend/list', method: 'get', params: query }) }
export function saveRecommend(data)     { return request({ url: '/business/ranking/recommend/save', method: 'post', data: data }) }
export function delRecommend(ids)       { return request({ url: '/business/ranking/recommend/' + ids, method: 'delete' }) }

// 融合展示榜
export function listDisplayRanking(query) { return request({ url: '/business/ranking/display/list', method: 'get', params: query }) }
export function delDisplayRanking(ids)    { return request({ url: '/business/ranking/display/' + ids, method: 'delete' }) }
export function regenerateDisplay()       { return request({ url: '/business/ranking/display/regenerate', method: 'post' }) }
