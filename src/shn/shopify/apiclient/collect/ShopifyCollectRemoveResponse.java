package shn.shopify.apiclient.collect;

import lombok.Data;
import lombok.experimental.Accessors;

import wbs.framework.data.annotations.DataClass;

import shn.shopify.apiclient.ShopifyApiResponse;

@Accessors (fluent = true)
@Data
@DataClass
public
class ShopifyCollectRemoveResponse
	implements ShopifyApiResponse {

}