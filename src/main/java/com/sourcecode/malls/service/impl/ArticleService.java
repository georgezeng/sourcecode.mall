package com.sourcecode.malls.service.impl;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sourcecode.malls.constants.CacheNameConstant;
import com.sourcecode.malls.context.ClientContext;
import com.sourcecode.malls.domain.article.Article;
import com.sourcecode.malls.domain.merchant.Merchant;
import com.sourcecode.malls.domain.redis.SearchCacheKeyStore;
import com.sourcecode.malls.dto.article.ArticleDTO;
import com.sourcecode.malls.repository.jpa.impl.article.ArticleRepository;
import com.sourcecode.malls.repository.jpa.impl.merchant.MerchantRepository;
import com.sourcecode.malls.repository.redis.impl.SearchCacheKeyStoreRepository;
import com.sourcecode.malls.util.AssertUtil;

@Service
@Transactional
public class ArticleService {

	@Autowired
	private ArticleRepository repository;

	@Autowired
	private MerchantRepository merchantRepository;

	@Autowired
	private SearchCacheKeyStoreRepository cacheStoreRepository;

	@Transactional(readOnly = true)
	@Cacheable(cacheNames = CacheNameConstant.ARTICLE_LOAD_ONE, key = "#merchantId + '-' + #title")
	public ArticleDTO search(Long merchantId, String title) {
		SearchCacheKeyStore store = new SearchCacheKeyStore();
		store.setType(SearchCacheKeyStore.SEARCH_ARTICLE);
		store.setBizKey(merchantId + "-" + title);
		store.setSearchKey(title);
		cacheStoreRepository.save(store);
		Optional<Merchant> merchant = merchantRepository.findById(ClientContext.getMerchantId());
		AssertUtil.assertTrue(merchant.isPresent(), "商家不存在");
		Optional<Article> data = repository.findFirstByMerchantAndTitleOrderByUpdateTimeDesc(merchant.get(), title);
		ArticleDTO dto = null;
		if (data.isPresent()) {
			dto = data.get().asDTO();
		}
		return dto;
	}

}
