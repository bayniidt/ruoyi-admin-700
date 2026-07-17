1: https://docs.partnerstack.com/reference/get_v2-customers-1

Query Params
min_created
int64
Applies a minimum epoch timestamp (ms) filter to the response created_at.

max_created
int64
Applies a maximum epoch timestamp (ms) filter to the response created_at.

order_by
string
enum
The field in which the results are ordered by. A - prefix denotes that the results are in descending order.


-created_at
Allowed:

-created_at

created_at
limit
integer
1 to 250
Defaults to 10
A limit on the number of items to be returned. Limits can range between 1 and 250, and the default is 10.

10
starting_after
string
A cursor for use in pagination, starting_after takes in an item key and the subsequent call will return the following items in the list. This is mutually exclusive with ending_before.

ending_before
string
A cursor for use in pagination, ending_before takes in an item key and the subsequent call will return the prior items in the list. This is mutually exclusive with starting_after.

Responses

200
Returns a list of your customers. The customers are returned sorted by creation date by default, with the most recent customers appearing first.

Response body
object
data
object
required
has_more
boolean
items
array of objects
object
Customers represent the users who have been referred by a Partner.


created_at
int64
Immutable Unix timestamp in milliseconds taken at time of creation

key
string
length ≤ 255
Unique key of this customer

updated_at
int64
Unix timestamp in milliseconds taken at time of last update

archived
boolean
The boolean flag which determines if the customer is archived or not

company
object
Base schema representation of a company


company object
key
string
Unique key of this company

name
string
The name of this company

country_iso
string | null
The abbreviated name of the customer's country of origin

customer_key
string | null
The key provided by the company to identify the customer

has_paid
boolean
The boolean flag which determines if the customer has paid or not

partnership_key
string
Unique key of this partnership

shared_id
string | null
The shared id of the customer

sub_ids
array of strings
total
integer | null
message
string
required
status
int32
required

响应数据结构：
{
  "data": {
    "has_more": true,
    "items": [
      {
        "archived": false,
        "company": {
          "key": "co_MCf1xvxSbKw35R",
          "name": "TikTok for Business"
        },
        "country_iso": "TH",
        "created_at": 1784100202000,
        "customer_key": "7662651690235002887",
        "has_paid": false,
        "key": "cus_Ak7az1vjG4iNux",
        "partnership_key": "part_FNiYCFmn1w3OHb",
        "shared_id": null,
        "sub_ids": [],
        "updated_at": 1784217960934
      },
      {
        "archived": false,
        "company": {
          "key": "co_MCf1xvxSbKw35R",
          "name": "TikTok for Business"
        },
        "country_iso": "TH",
        "created_at": 1784099999000,
        "customer_key": "7662651059571441681",
        "has_paid": false,
        "key": "cus_fBoIb1y0W5ivQf",
        "partnership_key": "part_FNiYCFmn1w3OHb",
        "shared_id": null,
        "sub_ids": [],
        "updated_at": 1784218404969
      },
      {
        "archived": false,
        "company": {
          "key": "co_MCf1xvxSbKw35R",
          "name": "TikTok for Business"
        },
        "country_iso": "PH",
        "created_at": 1783939086000,
        "customer_key": "7661957723463598101",
        "has_paid": false,
        "key": "cus_czePzc2qJP5X2M",
        "partnership_key": "part_FNiYCFmn1w3OHb",
        "shared_id": null,
        "sub_ids": [],
        "updated_at": 1784043782520
      },
      {
        "archived": false,
        "company": {
          "key": "co_MCf1xvxSbKw35R",
          "name": "TikTok for Business"
        },
        "country_iso": "TH",
        "created_at": 1783935029000,
        "customer_key": "7661942193825005586",
        "has_paid": false,
        "key": "cus_U2eTXoldvOyYOi",
        "partnership_key": "part_FNiYCFmn1w3OHb",
        "shared_id": null,
        "sub_ids": [],
        "updated_at": 1784043706876
      },
      {
        "archived": false,
        "company": {
          "key": "co_MCf1xvxSbKw35R",
          "name": "TikTok for Business"
        },
        "country_iso": "TH",
        "created_at": 1783934936000,
        "customer_key": "7661942101459550209",
        "has_paid": false,
        "key": "cus_9kzmTaIotiqMOy",
        "partnership_key": "part_FNiYCFmn1w3OHb",
        "shared_id": null,
        "sub_ids": [],
        "updated_at": 1784043901898
      },
      {
        "archived": false,
        "company": {
          "key": "co_MCf1xvxSbKw35R",
          "name": "TikTok for Business"
        },
        "country_iso": "TH",
        "created_at": 1783934766000,
        "customer_key": "7661941394558304272",
        "has_paid": false,
        "key": "cus_0Qomh0WvaWvWXN",
        "partnership_key": "part_FNiYCFmn1w3OHb",
        "shared_id": null,
        "sub_ids": [],
        "updated_at": 1784044213814
      },
      {
        "archived": false,
        "company": {
          "key": "co_MCf1xvxSbKw35R",
          "name": "TikTok for Business"
        },
        "country_iso": "TH",
        "created_at": 1783932482000,
        "customer_key": "7661931264418152455",
        "has_paid": false,
        "key": "cus_GgRQWmjzoczCf3",
        "partnership_key": "part_FNiYCFmn1w3OHb",
        "shared_id": null,
        "sub_ids": [],
        "updated_at": 1784044428161
      },
      {
        "archived": false,
        "company": {
          "key": "co_MCf1xvxSbKw35R",
          "name": "TikTok for Business"
        },
        "country_iso": "US",
        "created_at": 1783749193000,
        "customer_key": "7661141855836569620",
        "has_paid": false,
        "key": "cus_Cg6sUQ010fjD8k",
        "partnership_key": "part_FNiYCFmn1w3OHb",
        "shared_id": null,
        "sub_ids": [],
        "updated_at": 1783862485798
      },
      {
        "archived": false,
        "company": {
          "key": "co_MCf1xvxSbKw35R",
          "name": "TikTok for Business"
        },
        "country_iso": "US",
        "created_at": 1783748823000,
        "customer_key": "7661142762896539664",
        "has_paid": false,
        "key": "cus_t3ivF9ExJAxSk9",
        "partnership_key": "part_FNiYCFmn1w3OHb",
        "shared_id": null,
        "sub_ids": [],
        "updated_at": 1783862610540
      },
      {
        "archived": false,
        "company": {
          "key": "co_MCf1xvxSbKw35R",
          "name": "TikTok for Business"
        },
        "country_iso": "TH",
        "created_at": 1783670923000,
        "customer_key": "7660807080453406727",
        "has_paid": false,
        "key": "cus_6mwLzH8nudIhcX",
        "partnership_key": "part_FNiYCFmn1w3OHb",
        "shared_id": null,
        "sub_ids": [],
        "updated_at": 1783779161317
      }
    ],
    "total": 33
  },
  "message": "Retrieved customers successfully",
  "status": 200
}


2. https://docs.partnerstack.com/reference/get_v2-actions：
Query Params
min_created
int64
Applies a minimum epoch timestamp (ms) filter to the response created_at.

max_created
int64
Applies a maximum epoch timestamp (ms) filter to the response created_at.

customer_key
string
Customer key in which the results are filtered by

order_by
string
enum
The field in which the results are ordered by. A - prefix denotes that the results are in descending order.


-created_at
Allowed:

-created_at

created_at
limit
integer
1 to 250
Defaults to 10
A limit on the number of items to be returned. Limits can range between 1 and 250, and the default is 10.

10
starting_after
string
A cursor for use in pagination, starting_after takes in an item key and the subsequent call will return the following items in the list. This is mutually exclusive with ending_before.

ending_before
string
A cursor for use in pagination, ending_before takes in an item key and the subsequent call will return the prior items in the list. This is mutually exclusive with starting_after.

Responses

200
Returns a list of your actions. The actions are returned sorted by creation date by default, with the most recent actions appearing first.

Response body
object
data
object
required
has_more
boolean
items
array of objects
object
An Action represents a specific action a partner or customer took on your platform


created_at
int64
Immutable Unix timestamp in milliseconds taken at time of creation

key
string
length ≤ 255
A unique identifier used to reference the object

updated_at
int64
Unix timestamp in milliseconds taken at time of last update

archived
boolean
The boolean flag which determines if the action is archived or not.

company
object
Base schema representation of a company


company object
key
string
Unique key of this company

name
string
The name of this company

customer
object | null

customer object | null
key
string | null
Unique key of this customer.

sub_ids
array of strings | null
partnership_key
string
Unique key of this partnership.

type
string
The type of action the partner or customer performed

value
integer
≥ 1
The number of times the action was performed

total
integer | null
message
string
required
status
int32
required


响应数据结构：
{
  "data": {
    "has_more": true,
    "items": [
      {
        "archived": false,
        "company": {
          "key": "co_MCf1xvxSbKw35R",
          "name": "TikTok for Business"
        },
        "created_at": 1784119626000,
        "customer": {
          "key": "cus_Ebvgs2n9lfVvE1",
          "shared_id": null,
          "sub_ids": []
        },
        "key": "act_PP3vbJSbOVkG93",
        "partnership_key": "part_FNiYCFmn1w3OHb",
        "type": "top_up",
        "updated_at": 1784221656835,
        "value": 1
      },
      {
        "archived": false,
        "company": {
          "key": "co_MCf1xvxSbKw35R",
          "name": "TikTok for Business"
        },
        "created_at": 1784100202000,
        "customer": {
          "key": "cus_Ak7az1vjG4iNux",
          "shared_id": null,
          "sub_ids": []
        },
        "key": "act_2KFBqUxpG4XbGJ",
        "partnership_key": "part_FNiYCFmn1w3OHb",
        "type": "sign_up",
        "updated_at": 1784217965392,
        "value": 1
      },
      {
        "archived": false,
        "company": {
          "key": "co_MCf1xvxSbKw35R",
          "name": "TikTok for Business"
        },
        "created_at": 1784099999000,
        "customer": {
          "key": "cus_fBoIb1y0W5ivQf",
          "shared_id": null,
          "sub_ids": []
        },
        "key": "act_H8w7T03NdP3gcZ",
        "partnership_key": "part_FNiYCFmn1w3OHb",
        "type": "sign_up",
        "updated_at": 1784218431213,
        "value": 1
      },
      {
        "archived": false,
        "company": {
          "key": "co_MCf1xvxSbKw35R",
          "name": "TikTok for Business"
        },
        "created_at": 1784084401000,
        "customer": {
          "key": "cus_ECQ5FiFPzZB9j5",
          "shared_id": null,
          "sub_ids": []
        },
        "key": "act_VY8Ysi5ZC1rknK",
        "partnership_key": "part_FNiYCFmn1w3OHb",
        "type": "top_up",
        "updated_at": 1784221102862,
        "value": 1
      },
      {
        "archived": false,
        "company": {
          "key": "co_MCf1xvxSbKw35R",
          "name": "TikTok for Business"
        },
        "created_at": 1784008667000,
        "customer": {
          "key": "cus_Ebvgs2n9lfVvE1",
          "shared_id": null,
          "sub_ids": []
        },
        "key": "act_bAIJuMIFgmQttE",
        "partnership_key": "part_FNiYCFmn1w3OHb",
        "type": "top_up",
        "updated_at": 1784125612296,
        "value": 1
      },
      {
        "archived": false,
        "company": {
          "key": "co_MCf1xvxSbKw35R",
          "name": "TikTok for Business"
        },
        "created_at": 1784006301000,
        "customer": {
          "key": "cus_ECQ5FiFPzZB9j5",
          "shared_id": null,
          "sub_ids": []
        },
        "key": "act_MlT54xRw6IefZl",
        "partnership_key": "part_FNiYCFmn1w3OHb",
        "type": "top_up",
        "updated_at": 1784124940829,
        "value": 1
      },
      {
        "archived": false,
        "company": {
          "key": "co_MCf1xvxSbKw35R",
          "name": "TikTok for Business"
        },
        "created_at": 1784003948000,
        "customer": {
          "key": "cus_w49zFXUyRtipTm",
          "shared_id": null,
          "sub_ids": []
        },
        "key": "act_rO7axjfDgw0FKb",
        "partnership_key": "part_FNiYCFmn1w3OHb",
        "type": "top_up",
        "updated_at": 1784125759417,
        "value": 1
      },
      {
        "archived": false,
        "company": {
          "key": "co_MCf1xvxSbKw35R",
          "name": "TikTok for Business"
        },
        "created_at": 1783999023000,
        "customer": {
          "key": "cus_x2k45Jcw5cFWfQ",
          "shared_id": null,
          "sub_ids": []
        },
        "key": "act_18nmlNwxQ2LEKA",
        "partnership_key": "part_FNiYCFmn1w3OHb",
        "type": "top_up",
        "updated_at": 1784125160482,
        "value": 1
      },
      {
        "archived": false,
        "company": {
          "key": "co_MCf1xvxSbKw35R",
          "name": "TikTok for Business"
        },
        "created_at": 1783990668000,
        "customer": {
          "key": "cus_yc0cLypBFrvVbg",
          "shared_id": null,
          "sub_ids": []
        },
        "key": "act_wgv0wem95DflL9",
        "partnership_key": "part_FNiYCFmn1w3OHb",
        "type": "top_up",
        "updated_at": 1784125701644,
        "value": 1
      },
      {
        "archived": false,
        "company": {
          "key": "co_MCf1xvxSbKw35R",
          "name": "TikTok for Business"
        },
        "created_at": 1783980000000,
        "customer": {
          "key": "cus_ECQ5FiFPzZB9j5",
          "shared_id": null,
          "sub_ids": []
        },
        "key": "act_Vz9JnBHH22iRut",
        "partnership_key": "part_FNiYCFmn1w3OHb",
        "type": "new_high_value_advertiser",
        "updated_at": 1784050388977,
        "value": 1
      }
    ],
    "total": 84
  },
  "message": "Actions retrieved successfully",
  "status": 200
}


3. https://docs.partnerstack.com/reference/get_v2-transactions：
Query Params
min_created
int64
Applies a minimum epoch timestamp (ms) filter to the response created_at.

max_created
int64
Applies a maximum epoch timestamp (ms) filter to the response created_at.

order_by
string
enum
The field in which the results are ordered by. A - prefix denotes that the results are in descending order.


-created_at
Allowed:

-created_at

created_at
limit
integer
1 to 250
Defaults to 10
A limit on the number of items to be returned. Limits can range between 1 and 250, and the default is 10.

10
starting_after
string
A cursor for use in pagination, starting_after takes in an item key and the subsequent call will return the following items in the list. This is mutually exclusive with ending_before.

ending_before
string
A cursor for use in pagination, ending_before takes in an item key and the subsequent call will return the prior items in the list. This is mutually exclusive with starting_after.

Responses

200
Returns a list of your transactions. The transactions are returned sorted by creation date, with the most recent transactions appearing first.

Response body
object
data
object
required
has_more
boolean
items
array of objects
object
Transactions


created_at
int64
Immutable Unix timestamp in milliseconds taken at time of creation

key
string
length ≤ 255
A unique identifier used to reference the object

updated_at
int64
Unix timestamp in milliseconds taken at time of last update

amount
integer
Transaction amount in cents for specified currency.

amount_usd
integer
Transaction amount in cents converted to USD.

archived
boolean
The boolean flag which determines if the transaction is archived or not

category_key
string | null
Key of transaction category.

company
object
Base schema representation of a company


company object
key
string
Unique key of this company

name
string
The name of this company

currency
string
length between 3 and 3
Transaction currency type.

customer
object | null

customer object | null
key
string | null
Unique key of this customer.

shared_id
string | null
A shared ID that can be used to reference the customer.

sub_ids
array of strings | null
partnership_key
string
Unique key of this partnership.

product_key
string | null
Key of transaction product.

total
integer | null
message
string
required
status
int32
required


响应数据结构：
{
  "data": {
    "has_more": true,
    "items": [
      {
        "amount": 45892,
        "amount_usd": 45892,
        "archived": false,
        "category_key": null,
        "company": {
          "key": "co_MCf1xvxSbKw35R",
          "name": "TikTok for Business"
        },
        "created_at": 1784152800000,
        "currency": "USD",
        "customer": {
          "key": "cus_yc0cLypBFrvVbg",
          "shared_id": null,
          "sub_ids": []
        },
        "key": "7659610663496810517_101_1_20260715",
        "partnership_key": "part_FNiYCFmn1w3OHb",
        "product_key": null,
        "updated_at": 1784224596422
      },
      {
        "amount": 3031,
        "amount_usd": 3031,
        "archived": false,
        "category_key": null,
        "company": {
          "key": "co_MCf1xvxSbKw35R",
          "name": "TikTok for Business"
        },
        "created_at": 1784152800000,
        "currency": "USD",
        "customer": {
          "key": "cus_w49zFXUyRtipTm",
          "shared_id": null,
          "sub_ids": []
        },
        "key": "7660430919907557397_101_1_20260715",
        "partnership_key": "part_FNiYCFmn1w3OHb",
        "product_key": null,
        "updated_at": 1784224586346
      },
      {
        "amount": 16102,
        "amount_usd": 16102,
        "archived": false,
        "category_key": null,
        "company": {
          "key": "co_MCf1xvxSbKw35R",
          "name": "TikTok for Business"
        },
        "created_at": 1784152800000,
        "currency": "USD",
        "customer": {
          "key": "cus_ECQ5FiFPzZB9j5",
          "shared_id": null,
          "sub_ids": []
        },
        "key": "7660410117636440082_101_1_20260715",
        "partnership_key": "part_FNiYCFmn1w3OHb",
        "product_key": null,
        "updated_at": 1784224106483
      },
      {
        "amount": 25883,
        "amount_usd": 25883,
        "archived": false,
        "category_key": null,
        "company": {
          "key": "co_MCf1xvxSbKw35R",
          "name": "TikTok for Business"
        },
        "created_at": 1784152800000,
        "currency": "USD",
        "customer": {
          "key": "cus_7lOYr6TxgV5rmp",
          "shared_id": null,
          "sub_ids": []
        },
        "key": "7659734376678162452_101_1_20260715",
        "partnership_key": "part_FNiYCFmn1w3OHb",
        "product_key": null,
        "updated_at": 1784223117148
      },
      {
        "amount": 40533,
        "amount_usd": 40533,
        "archived": false,
        "category_key": null,
        "company": {
          "key": "co_MCf1xvxSbKw35R",
          "name": "TikTok for Business"
        },
        "created_at": 1784066400000,
        "currency": "USD",
        "customer": {
          "key": "cus_7lOYr6TxgV5rmp",
          "shared_id": null,
          "sub_ids": []
        },
        "key": "7659734376678162452_101_1_20260714",
        "partnership_key": "part_FNiYCFmn1w3OHb",
        "product_key": null,
        "updated_at": 1784130421145
      },
      {
        "amount": 5227,
        "amount_usd": 5227,
        "archived": false,
        "category_key": null,
        "company": {
          "key": "co_MCf1xvxSbKw35R",
          "name": "TikTok for Business"
        },
        "created_at": 1784066400000,
        "currency": "USD",
        "customer": {
          "key": "cus_w49zFXUyRtipTm",
          "shared_id": null,
          "sub_ids": []
        },
        "key": "7660430919907557397_101_1_20260714",
        "partnership_key": "part_FNiYCFmn1w3OHb",
        "product_key": null,
        "updated_at": 1784130145505
      },
      {
        "amount": 64583,
        "amount_usd": 64583,
        "archived": false,
        "category_key": null,
        "company": {
          "key": "co_MCf1xvxSbKw35R",
          "name": "TikTok for Business"
        },
        "created_at": 1784066400000,
        "currency": "USD",
        "customer": {
          "key": "cus_yc0cLypBFrvVbg",
          "shared_id": null,
          "sub_ids": []
        },
        "key": "7659610663496810517_101_1_20260714",
        "partnership_key": "part_FNiYCFmn1w3OHb",
        "product_key": null,
        "updated_at": 1784130130456
      },
      {
        "amount": 27135,
        "amount_usd": 27135,
        "archived": false,
        "category_key": null,
        "company": {
          "key": "co_MCf1xvxSbKw35R",
          "name": "TikTok for Business"
        },
        "created_at": 1784066400000,
        "currency": "USD",
        "customer": {
          "key": "cus_x2k45Jcw5cFWfQ",
          "shared_id": null,
          "sub_ids": []
        },
        "key": "7660407238843760658_101_1_20260714",
        "partnership_key": "part_FNiYCFmn1w3OHb",
        "product_key": null,
        "updated_at": 1784129976192
      },
      {
        "amount": 19023,
        "amount_usd": 19023,
        "archived": false,
        "category_key": null,
        "company": {
          "key": "co_MCf1xvxSbKw35R",
          "name": "TikTok for Business"
        },
        "created_at": 1784066400000,
        "currency": "USD",
        "customer": {
          "key": "cus_ECQ5FiFPzZB9j5",
          "shared_id": null,
          "sub_ids": []
        },
        "key": "7660410117636440082_101_1_20260714",
        "partnership_key": "part_FNiYCFmn1w3OHb",
        "product_key": null,
        "updated_at": 1784129860055
      },
      {
        "amount": 36209,
        "amount_usd": 36209,
        "archived": false,
        "category_key": null,
        "company": {
          "key": "co_MCf1xvxSbKw35R",
          "name": "TikTok for Business"
        },
        "created_at": 1783980000000,
        "currency": "USD",
        "customer": {
          "key": "cus_7lOYr6TxgV5rmp",
          "shared_id": null,
          "sub_ids": []
        },
        "key": "7659734376678162452_101_1_20260713",
        "partnership_key": "part_FNiYCFmn1w3OHb",
        "product_key": null,
        "updated_at": 1784051714233
      }
    ]
  },
  "message": "Transactions retrieved successfully",
  "status": 200
}



4. https://docs.partnerstack.com/reference/get_v2-rewards 
Query Params
company_key
string
Company key in which the results are filtered by.

payment_status
string
enum
The payment status of the rewards


available
Allowed:

in_transit

withdrawn

available

paid_externally

expired

failed

merging
max_created
int64
Applies a maximum epoch timestamp (ms) filter to the response created_at.

min_created
int64
Applies a minimum epoch timestamp (ms) filter to the response created_at.

order_by
string
enum
The field in which the results are ordered by. A - prefix denotes that the results are in descending order.


-created_at
Allowed:

-created_at

created_at

-amount

amount

-ready_at

ready_at
group_key
string
Group key in which the results are filtered by

customer_key
string
Customer key in which the results are filtered by

invoice_key
string
Invoice key in which the results are filtered by

status
string
The status of the rewards

exclude_drip_rewards
string
Flag if drip rewards should be included into the results

hide_archived_rewards
boolean
Flag to hide rewards that have been archived


false
empty_line_id
boolean
Flag if drip rewards returned have a line_id (invoice) attached or not


false
keywords
string
Free keywords that will filter on the followingReward key, Customer name, Partner first name, Partner last name, Partner email, Partner key

description
string
Text that the description must contain

distinct_description
boolean
Flag the returned results should have distinct descriptions or not


true
distinct_decline_reason
boolean
Flag the returned results should have distinct decline reasons or not


true
decline_reason
string
Text that the decline_reason must contain

invoiceable
boolean
Flag if the returned results should have only pending and approved rewards which do not have an associated payment line.


true
currency
string
The currency of the rewards

limit
integer
1 to 250
Defaults to 10
A limit on the number of items to be returned. Limits can range between 1 and 250, and the default is 10.

10
starting_after
string
A cursor for use in pagination, starting_after takes in an item key and the subsequent call will return the following items in the list. This is mutually exclusive with ending_before.

ending_before
string
A cursor for use in pagination, ending_before takes in an item key and the subsequent call will return the prior items in the list. This is mutually exclusive with starting_after.

Responses

200
Returns a list of your rewards. The rewards are returned sorted by creation date by default, with the most recent rewards appearing first.

Response body
object
data
object
required
has_more
boolean
items
array of objects
object
Reward is what you earn from companies on PartnerStack.


created_at
int64
Immutable Unix timestamp in milliseconds taken at time of creation

key
string
length ≤ 255
A unique identifier used to reference the object

updated_at
int64
Unix timestamp in milliseconds taken at time of last update

amount
integer | null
≤ 100000000
The amount of the reward in cents (USD).

currency
string | null
The currency of the reward.

decline_reason
string | null
The reason the reward has been held or declined

description
string | null
The description that the company gives to the reward.

reward_status
string | null
The status of the reward. Can be one of: hold, pending, approved, declined, paid.

company
object

company object
id
integer
Unique id of this company.

key
string
Unique key of this company.

name
string
The name of this company.

customer
object | null

customer object | null
created_at
integer | null
Immutable Unix timestamp in milliseconds taken at time of customer creation

email
string | null
The email of the customer.

key
string | null
Unique key of this customer.

name
string | null
The name of the customer.

sub_ids
array of strings | null
updated_at
integer | null
Immutable Unix timestamp in milliseconds taken at time of customer last update

partnership_key
string | null
Unique key of this partnership.

payment_date
integer | null
The epoch timestamp (ms) for when the partner should be able to cash out the reward. Can be null when the reward is declined.

payment_status
string | null
This status indicates whether the partner can withdraw the reward. Can be one of: pending, available, withdrawn, or paid_externally. Can be null when the payment is declined.

payout_id
integer | null
The unique Id of the payout this reward is associted with.

source
object | null

source object | null
key
string
The key of the source that generated this reward.

type
string | null
The source that generated this reward. Can be one of: transaction, lead, deal, submission, action, bill, asset, order_item, request, bonus.

transaction
object | null

transaction object | null
amount
integer | null
Transaction amount in cents for specified currency.

amount_usd
integer | null
Transaction amount in cents converted to USD.

archived
boolean
The boolean flag which determines if the transaction is archived or not

category_key
string | null
Key of transaction category.

created_at
integer | null
Immutable Unix timestamp in milliseconds taken at time of creation

currency
string | null
Transaction currency type.

product_key
string | null
Key of transaction product.

updated_at
integer | null
Unix timestamp in milliseconds taken at time of last update

withdrawn
boolean
The boolean flag which determines if the reward has been withdrawn already in some payout

total
integer | null
message
string
required
status
int32
required


响应数据结构：
{
  "data": {
    "has_more": true,
    "items": [
      {
        "amount": 9178,
        "company": {
          "id": 10657,
          "key": "co_MCf1xvxSbKw35R",
          "name": "TikTok for Business"
        },
        "created_at": 1784224700937,
        "created_by": null,
        "created_by_name": null,
        "creation_source": null,
        "currency": "USD",
        "customer": {
          "created_at": 1783392401000,
          "email": null,
          "key": "cus_yc0cLypBFrvVbg",
          "name": null,
          "shared_id": null,
          "sub_ids": [],
          "updated_at": 1784130130580
        },
        "decline_reason": null,
        "description": "Member - 20% commission for first 28 days after cash active - $458.92 USD purchase by None",
        "key": "rwrd_y3VcW6BYJuXzIC",
        "partnership_key": "part_FNiYCFmn1w3OHb",
        "payment_date": 1787011200000,
        "payment_status": null,
        "payout_id": null,
        "reward_status": "pending",
        "source": {
          "key": "7659610663496810517_101_1_20260715",
          "type": "transaction"
        },
        "transaction": {
          "amount": 45892,
          "amount_usd": 45892,
          "archived": false,
          "category_key": null,
          "created_at": 1784152800000,
          "currency": "USD",
          "product_key": null,
          "updated_at": 1784224596422
        },
        "updated_at": 1784224700937,
        "withdrawn": false
      },
      {
        "amount": 606,
        "company": {
          "id": 10657,
          "key": "co_MCf1xvxSbKw35R",
          "name": "TikTok for Business"
        },
        "created_at": 1784224699410,
        "created_by": null,
        "created_by_name": null,
        "creation_source": null,
        "currency": "USD",
        "customer": {
          "created_at": 1783583417000,
          "email": null,
          "key": "cus_w49zFXUyRtipTm",
          "name": null,
          "shared_id": null,
          "sub_ids": [],
          "updated_at": 1784130145648
        },
        "decline_reason": null,
        "description": "Member - 20% commission for first 28 days after cash active - $30.31 USD purchase by None",
        "key": "rwrd_1KFA9EWOSU40of",
        "partnership_key": "part_FNiYCFmn1w3OHb",
        "payment_date": 1787011200000,
        "payment_status": null,
        "payout_id": null,
        "reward_status": "pending",
        "source": {
          "key": "7660430919907557397_101_1_20260715",
          "type": "transaction"
        },
        "transaction": {
          "amount": 3031,
          "amount_usd": 3031,
          "archived": false,
          "category_key": null,
          "created_at": 1784152800000,
          "currency": "USD",
          "product_key": null,
          "updated_at": 1784224586346
        },
        "updated_at": 1784224699410,
        "withdrawn": false
      },
      {
        "amount": 3220,
        "company": {
          "id": 10657,
          "key": "co_MCf1xvxSbKw35R",
          "name": "TikTok for Business"
        },
        "created_at": 1784224183374,
        "created_by": null,
        "created_by_name": null,
        "creation_source": null,
        "currency": "USD",
        "customer": {
          "created_at": 1783578351000,
          "email": null,
          "key": "cus_ECQ5FiFPzZB9j5",
          "name": null,
          "shared_id": null,
          "sub_ids": [],
          "updated_at": 1784224116881
        },
        "decline_reason": null,
        "description": "Member - 20% commission for first 28 days after cash active - $161.02 USD purchase by None",
        "key": "rwrd_bCCIwB2KW49Z0Z",
        "partnership_key": "part_FNiYCFmn1w3OHb",
        "payment_date": 1787011200000,
        "payment_status": null,
        "payout_id": null,
        "reward_status": "pending",
        "source": {
          "key": "7660410117636440082_101_1_20260715",
          "type": "transaction"
        },
        "transaction": {
          "amount": 16102,
          "amount_usd": 16102,
          "archived": false,
          "category_key": null,
          "created_at": 1784152800000,
          "currency": "USD",
          "product_key": null,
          "updated_at": 1784224106483
        },
        "updated_at": 1784224183374,
        "withdrawn": false
      },
      {
        "amount": 5177,
        "company": {
          "id": 10657,
          "key": "co_MCf1xvxSbKw35R",
          "name": "TikTok for Business"
        },
        "created_at": 1784223242835,
        "created_by": null,
        "created_by_name": null,
        "creation_source": null,
        "currency": "USD",
        "customer": {
          "created_at": 1783421064000,
          "email": null,
          "key": "cus_7lOYr6TxgV5rmp",
          "name": null,
          "shared_id": null,
          "sub_ids": [],
          "updated_at": 1784130421829
        },
        "decline_reason": null,
        "description": "Member - 20% commission for first 28 days after cash active - $258.83 USD purchase by None",
        "key": "rwrd_fCOuvrbFlaQsdt",
        "partnership_key": "part_FNiYCFmn1w3OHb",
        "payment_date": 1787011200000,
        "payment_status": null,
        "payout_id": null,
        "reward_status": "pending",
        "source": {
          "key": "7659734376678162452_101_1_20260715",
          "type": "transaction"
        },
        "transaction": {
          "amount": 25883,
          "amount_usd": 25883,
          "archived": false,
          "category_key": null,
          "created_at": 1784152800000,
          "currency": "USD",
          "product_key": null,
          "updated_at": 1784223117148
        },
        "updated_at": 1784223242835,
        "withdrawn": false
      },
      {
        "amount": 8107,
        "company": {
          "id": 10657,
          "key": "co_MCf1xvxSbKw35R",
          "name": "TikTok for Business"
        },
        "created_at": 1784130422329,
        "created_by": null,
        "created_by_name": null,
        "creation_source": null,
        "currency": "USD",
        "customer": {
          "created_at": 1783421064000,
          "email": null,
          "key": "cus_7lOYr6TxgV5rmp",
          "name": null,
          "shared_id": null,
          "sub_ids": [],
          "updated_at": 1784130421829
        },
        "decline_reason": null,
        "description": "Member - 20% commission for first 28 days after cash active - $405.33 USD purchase by None",
        "key": "rwrd_02oPiGzAY5Jp1o",
        "partnership_key": "part_FNiYCFmn1w3OHb",
        "payment_date": 1787011200000,
        "payment_status": null,
        "payout_id": null,
        "reward_status": "pending",
        "source": {
          "key": "7659734376678162452_101_1_20260714",
          "type": "transaction"
        },
        "transaction": {
          "amount": 40533,
          "amount_usd": 40533,
          "archived": false,
          "category_key": null,
          "created_at": 1784066400000,
          "currency": "USD",
          "product_key": null,
          "updated_at": 1784130421145
        },
        "updated_at": 1784130422329,
        "withdrawn": false
      },
      {
        "amount": 1045,
        "company": {
          "id": 10657,
          "key": "co_MCf1xvxSbKw35R",
          "name": "TikTok for Business"
        },
        "created_at": 1784130146040,
        "created_by": null,
        "created_by_name": null,
        "creation_source": null,
        "currency": "USD",
        "customer": {
          "created_at": 1783583417000,
          "email": null,
          "key": "cus_w49zFXUyRtipTm",
          "name": null,
          "shared_id": null,
          "sub_ids": [],
          "updated_at": 1784130145648
        },
        "decline_reason": null,
        "description": "Member - 20% commission for first 28 days after cash active - $52.27 USD purchase by None",
        "key": "rwrd_2aLCrwyzUR4RaM",
        "partnership_key": "part_FNiYCFmn1w3OHb",
        "payment_date": 1787011200000,
        "payment_status": null,
        "payout_id": null,
        "reward_status": "pending",
        "source": {
          "key": "7660430919907557397_101_1_20260714",
          "type": "transaction"
        },
        "transaction": {
          "amount": 5227,
          "amount_usd": 5227,
          "archived": false,
          "category_key": null,
          "created_at": 1784066400000,
          "currency": "USD",
          "product_key": null,
          "updated_at": 1784130145505
        },
        "updated_at": 1784130146040,
        "withdrawn": false
      },
      {
        "amount": 12917,
        "company": {
          "id": 10657,
          "key": "co_MCf1xvxSbKw35R",
          "name": "TikTok for Business"
        },
        "created_at": 1784130131134,
        "created_by": null,
        "created_by_name": null,
        "creation_source": null,
        "currency": "USD",
        "customer": {
          "created_at": 1783392401000,
          "email": null,
          "key": "cus_yc0cLypBFrvVbg",
          "name": null,
          "shared_id": null,
          "sub_ids": [],
          "updated_at": 1784130130580
        },
        "decline_reason": null,
        "description": "Member - 20% commission for first 28 days after cash active - $645.83 USD purchase by None",
        "key": "rwrd_EFLtTcU7fwxWlb",
        "partnership_key": "part_FNiYCFmn1w3OHb",
        "payment_date": 1787011200000,
        "payment_status": null,
        "payout_id": null,
        "reward_status": "pending",
        "source": {
          "key": "7659610663496810517_101_1_20260714",
          "type": "transaction"
        },
        "transaction": {
          "amount": 64583,
          "amount_usd": 64583,
          "archived": false,
          "category_key": null,
          "created_at": 1784066400000,
          "currency": "USD",
          "product_key": null,
          "updated_at": 1784130130456
        },
        "updated_at": 1784130131134,
        "withdrawn": false
      },
      {
        "amount": 5427,
        "company": {
          "id": 10657,
          "key": "co_MCf1xvxSbKw35R",
          "name": "TikTok for Business"
        },
        "created_at": 1784129990739,
        "created_by": null,
        "created_by_name": null,
        "creation_source": null,
        "currency": "USD",
        "customer": {
          "created_at": 1783577737000,
          "email": null,
          "key": "cus_x2k45Jcw5cFWfQ",
          "name": null,
          "shared_id": null,
          "sub_ids": [],
          "updated_at": 1784051680983
        },
        "decline_reason": null,
        "description": "Member - 20% commission for first 28 days after cash active - $271.35 USD purchase by None",
        "key": "rwrd_3MtOxhTKbacPFZ",
        "partnership_key": "part_FNiYCFmn1w3OHb",
        "payment_date": 1787011200000,
        "payment_status": null,
        "payout_id": null,
        "reward_status": "pending",
        "source": {
          "key": "7660407238843760658_101_1_20260714",
          "type": "transaction"
        },
        "transaction": {
          "amount": 27135,
          "amount_usd": 27135,
          "archived": false,
          "category_key": null,
          "created_at": 1784066400000,
          "currency": "USD",
          "product_key": null,
          "updated_at": 1784129976192
        },
        "updated_at": 1784129990739,
        "withdrawn": false
      },
      {
        "amount": 3805,
        "company": {
          "id": 10657,
          "key": "co_MCf1xvxSbKw35R",
          "name": "TikTok for Business"
        },
        "created_at": 1784129860616,
        "created_by": null,
        "created_by_name": null,
        "creation_source": null,
        "currency": "USD",
        "customer": {
          "created_at": 1783578351000,
          "email": null,
          "key": "cus_ECQ5FiFPzZB9j5",
          "name": null,
          "shared_id": null,
          "sub_ids": [],
          "updated_at": 1784129860256
        },
        "decline_reason": null,
        "description": "Member - 20% commission for first 28 days after cash active - $190.23 USD purchase by None",
        "key": "rwrd_KcExQWHBnUV92u",
        "partnership_key": "part_FNiYCFmn1w3OHb",
        "payment_date": 1787011200000,
        "payment_status": null,
        "payout_id": null,
        "reward_status": "pending",
        "source": {
          "key": "7660410117636440082_101_1_20260714",
          "type": "transaction"
        },
        "transaction": {
          "amount": 19023,
          "amount_usd": 19023,
          "archived": false,
          "category_key": null,
          "created_at": 1784066400000,
          "currency": "USD",
          "product_key": null,
          "updated_at": 1784129860055
        },
        "updated_at": 1784129860616,
        "withdrawn": false
      },
      {
        "amount": 7242,
        "company": {
          "id": 10657,
          "key": "co_MCf1xvxSbKw35R",
          "name": "TikTok for Business"
        },
        "created_at": 1784054441279,
        "created_by": null,
        "created_by_name": null,
        "creation_source": null,
        "currency": "USD",
        "customer": {
          "created_at": 1783421064000,
          "email": null,
          "key": "cus_7lOYr6TxgV5rmp",
          "name": null,
          "shared_id": null,
          "sub_ids": [],
          "updated_at": 1784051714329
        },
        "decline_reason": null,
        "description": "Member - 20% commission for first 28 days after cash active - $362.09 USD purchase by None",
        "key": "rwrd_JND0Jq4QJuUWnI",
        "partnership_key": "part_FNiYCFmn1w3OHb",
        "payment_date": 1787011200000,
        "payment_status": null,
        "payout_id": null,
        "reward_status": "pending",
        "source": {
          "key": "7659734376678162452_101_1_20260713",
          "type": "transaction"
        },
        "transaction": {
          "amount": 36209,
          "amount_usd": 36209,
          "archived": false,
          "category_key": null,
          "created_at": 1783980000000,
          "currency": "USD",
          "product_key": null,
          "updated_at": 1784051714233
        },
        "updated_at": 1784054441279,
        "withdrawn": false
      }
    ]
  },
  "message": "Rewards page returned successfully",
  "status": 200
}
