package com.sourcecode.malls.context;

import com.sourcecode.malls.domain.client.Client;

public final class ClientContext {
	private static final ThreadLocal<Client> value = new ThreadLocal<>();
	private static final ThreadLocal<Long> merchantIdHolder = new ThreadLocal<>();

	public static Client get() {
		Client user = value.get();
		return user != null ? user : Client.SystemUser;
	}

	public static void set(Client user) {
		value.set(user);
	}

	public static Long getMerchantId() {
		return merchantIdHolder.get();
	}

	public static void setMerchantId(Long merchantId) {
		merchantIdHolder.set(merchantId);
	}
	
	public static void clear() {
		UserContext.set(null);
		set(null);
		setMerchantId(null);
	}

}
