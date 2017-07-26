package shn.shopify.apiclient.collect;

import static wbs.utils.collection.CollectionUtils.emptyList;

import java.util.List;

import lombok.Data;
import lombok.experimental.Accessors;

import wbs.framework.data.annotations.DataChildren;
import wbs.framework.data.annotations.DataClass;

import shn.shopify.apiclient.ShopifyApiResponse;
import shn.shopify.apiclient.metafield.ShopifyMetafieldResponse;

@Accessors (fluent = true)
@Data
@DataClass
public
class ShopifyCollectListResponse
	implements ShopifyApiResponse {

	@DataChildren (
		childrenElement = "collects")
	List <ShopifyCollectResponse> collects =
		emptyList ();

	@DataChildren (
		childrenElement = "metafields")
	List <ShopifyMetafieldResponse> metafields =
		emptyList ();

}
