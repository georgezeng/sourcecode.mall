package com.sourcecode.malls.service.impl;

import java.math.BigDecimal;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import com.sourcecode.malls.constants.CacheNameConstant;
import com.sourcecode.malls.domain.coupon.CouponSetting;
import com.sourcecode.malls.domain.merchant.Merchant;
import com.sourcecode.malls.dto.ClientInviteBonusInfo;
import com.sourcecode.malls.dto.client.ClientPointsBonus;
import com.sourcecode.malls.dto.merchant.SiteInfo;
import com.sourcecode.malls.enums.CouponEventType;
import com.sourcecode.malls.enums.CouponSettingStatus;
import com.sourcecode.malls.repository.jpa.impl.coupon.CouponSettingRepository;

@Service
public class MerchantService {
	@Autowired
	private MerchantSettingService settingService;

	@Autowired
	private CouponSettingRepository couponSettingRepository;

	@Cacheable(cacheNames = CacheNameConstant.MERCHANT_SITE_INFO, key = "#merchantId")
	public SiteInfo getSiteInfo(Long merchantId) throws Exception {
		return settingService.loadSiteInfo(merchantId);
	}

	@Cacheable(cacheNames = CacheNameConstant.CLIENT_POINTS_BONUS, key = "#merchantId")
	public ClientPointsBonus getClientPointsBonus(Long merchantId) throws Exception {
		return settingService.loadClientPointsBonus(merchantId);
	}

	@Cacheable(cacheNames = CacheNameConstant.CLIENT_INVITE_BONUS_INFO, key = "#merchant.id")
	public ClientInviteBonusInfo getInviteBonusInfo(Merchant merchant) throws Exception {
		ClientInviteBonusInfo info = new ClientInviteBonusInfo();
		Optional<CouponSetting> couponSetting = couponSettingRepository.findFirstByMerchantAndEventTypeAndStatusAndEnabledOrderByCreateTimeDesc(merchant,
				CouponEventType.Invite, CouponSettingStatus.PutAway, true);
		BigDecimal coupon = BigDecimal.ZERO;
		if (couponSetting.isPresent()) {
			coupon = couponSetting.get().getAmount();
		}
		info.setCoupon(coupon);
		ClientPointsBonus pointsBonus = settingService.loadClientPointsBonus(merchant.getId());
		info.setPoints(pointsBonus.getInvite());
		return info;
	}
}
