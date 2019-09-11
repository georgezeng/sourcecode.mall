package com.sourcecode.malls.service.impl;

import java.math.BigDecimal;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import com.sourcecode.malls.constants.CacheNameConstant;
import com.sourcecode.malls.domain.coupon.CouponSetting;
import com.sourcecode.malls.domain.merchant.Merchant;
import com.sourcecode.malls.dto.ClientBonusInfo;
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

	@Cacheable(cacheNames = CacheNameConstant.CLIENT_BONUS_INFO, key = "#merchant.id")
	public ClientBonusInfo getBonusInfo(Merchant merchant) throws Exception {
		ClientBonusInfo info = new ClientBonusInfo();
		Optional<CouponSetting> couponSetting = couponSettingRepository.findFirstByMerchantAndEventTypeAndStatusAndEnabledOrderByCreateTimeDesc(merchant,
				CouponEventType.Invite, CouponSettingStatus.PutAway, true);
		BigDecimal coupon = BigDecimal.ZERO;
		if (couponSetting.isPresent()) {
			coupon = couponSetting.get().getAmount();
		}
		info.setInviteCoupon(coupon);
		coupon = BigDecimal.ZERO;
		couponSetting = couponSettingRepository.findFirstByMerchantAndEventTypeAndStatusAndEnabledOrderByCreateTimeDesc(merchant,
				CouponEventType.Registration, CouponSettingStatus.PutAway, true);
		if (couponSetting.isPresent()) {
			coupon = couponSetting.get().getAmount();
		}
		info.setRookieCoupon(coupon);
		ClientPointsBonus pointsBonus = settingService.loadClientPointsBonus(merchant.getId());
		info.setInvitePoints(pointsBonus.getInvite());
		info.setRookiePoints(pointsBonus.getRookie());
		return info;
	}
}
