import request from '@/utils/request'

// 分类
export function listPortalCategories() { return request({ url: '/portal/category/list', method: 'get' }) }
// 商品
export function pagePortalProducts(query) { return request({ url: '/portal/product/page', method: 'get', params: query }) }
export function getPortalProduct(id) { return request({ url: '/portal/product/' + id, method: 'get' }) }
export function searchPortalProducts(query) { return request({ url: '/portal/product/search', method: 'get', params: query }) }
// AI对话
export function portalChat(data) { return request({ url: '/ai/chat', method: 'post', data: data }) }
export function portalChatSession() { return request({ url: '/ai/chat/session', method: 'post' }) }
export function portalChatToolData(sessionId) { return request({ url: '/ai/chat/tool-data?sessionId=' + sessionId, method: 'get' }) }
export function clearAiSession(sessionId) { return request({ url: '/ai/chat/clear-session?sessionId=' + sessionId, method: 'get' }) }
// 购物车
export function addToCart(data) { return request({ url: '/portal/cart/add', method: 'post', data: data }) }
export function getCartList(sessionId) { return request({ url: '/portal/cart/list?sessionId=' + sessionId, method: 'get' }) }
export function updateCartItem(data) { return request({ url: '/portal/cart/update', method: 'put', data: data }) }
export function removeCartItem(data) { return request({ url: '/portal/cart/remove', method: 'delete', data: data }) }
export function clearCart(sessionId) { return request({ url: '/portal/cart/clear', method: 'delete', data: { sessionId } }) }
// 订单
export function createPortalOrder(data) { return request({ url: '/portal/order/create', method: 'post', data: data }) }
export function queryPortalOrder(params) { return request({ url: '/portal/order/query', method: 'get', params: params }) }
export function getPortalOrder(orderNo) { return request({ url: '/portal/order/' + orderNo, method: 'get' }) }
