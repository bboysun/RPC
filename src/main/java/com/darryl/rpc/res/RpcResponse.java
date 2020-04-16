package com.darryl.rpc.res;

/**
 * @Auther: Darryl
 * @Description: 服务提供者接受到远程调用请求后，处理完成返回给远程客户端的response响应
 * @Date: 2020/04/16
 */
public class RpcResponse {
	private String result;

	private String interfaceMethodIdentify;

	private String requestId;

	public void setResult(String result) {
		this.result = result;
	}

	public static RpcResponse create(String result, String interfaceMethodIdentify, String requestId) {
		RpcResponse response = new RpcResponse();
		response.setResult(result);
		response.setInterfaceMethodIdentify(interfaceMethodIdentify);
		response.setRequestId(requestId);
		return response;
	}

	public String getResult() {
		return result;
	}

	public String getInterfaceMethodIdentify() {
		return interfaceMethodIdentify;
	}

	public void setInterfaceMethodIdentify(String interfaceMethodIdentify) {
		this.interfaceMethodIdentify = interfaceMethodIdentify;
	}

	public String getRequestId() {
		return requestId;
	}

	public void setRequestId(String requestId) {
		this.requestId = requestId;
	}
}
