package org.palliums.violascore.smartcontract.multitoken

import android.content.Context
import org.palliums.violascore.move.Move
import org.palliums.violascore.serialization.hexToBytes
import org.palliums.violascore.transaction.TransactionArgument
import org.palliums.violascore.transaction.TransactionPayload
import org.palliums.violascore.transaction.lbrStructTag

open class MultiTokenContract(
    private val contractAddress: String,
    private val multiContractRpcApi: MultiContractRpcApi?
) {
    companion object {
        private const val mTransferContract =
            "a11ceb0b010006013d0000000400000003410000000a000000054b000000070000000752000000210000000673000000100000000983000000140000000000000101020001000003000100040305030a0200063c53454c463e0b56696f6c6173546f6b656e087472616e73666572046d61696e7257c2417e4d1038e1817c8f283ace2e010000ffff030006000a000a010a020b03120002"

        private const val mPublishContract =
            "a11ceb0b010006013d0000000400000003410000000a000000054b00000004000000074f00000020000000066f00000010000000097f0000000e0000000000000101020001000003000100010a0200063c53454c463e0b56696f6c6173546f6b656e077075626c697368046d61696e7257c2417e4d1038e1817c8f283ace2e010000ffff030003000b00120002"

        private const val mMintContract =
            "a11ceb0b010006013d0000000400000003410000000a000000054b0000000700000007520000001d000000066f00000010000000097f000000140000000000000101020001000003000100040305030a0200063c53454c463e0b56696f6c6173546f6b656e046d696e74046d61696e7257c2417e4d1038e1817c8f283ace2e010000ffff030006000a000a010a020b03120002"

        private const val mTokenContract =
            "a11ceb0b01000a016100000008000000026900000025000000038e000000a400000004320100002600000005580100002b01000007830200005002000006d3040000200000000af3040000530000000c46050000df0400000d250a00000e0000000000010101020103000401000005010000060100000701000008010000090100000a0100000b02000213010102030c00010101030d02030101030e04050101030f010601010310070801010311090101010112030a010102140b0101020215010c010200160808000017010d0000180e080000190f0100001a100100001b0d0100001c111200001d130100001e140100001f150100002016010000210a010000220801000023010100002401010000251712000026010800002708080000281401000029180800002a191200002b08120004120112041c031d030a051c060d001d0608021207200512021c0312082003280308031c011c02070a09000a09000002060a0900030106090002070a09000301070900010a090001060a0900010302070a09000900010a0202070b080109000900010b08010900010502050a020205080203030a020a020208020802010802020708020802040305030a020303050a020303050302070802030106080202030304030608020306080505050a02030708040a02010803010206050802030708020708050304030a020a0207080601080705050307080407080503080802080203030103030306070802080203010303080305030a0208020708030708040a020603050a020708030708040a02040305030802040a020501030108000803030103010306080307080402010304070802030103010608040303060803060804050305030a020a0206030307080201030708050b56696f6c6173546f6b656e034c43530c4c696272614163636f756e7406566563746f72054f726465720a53757065727669736f72015409546f6b656e496e666f0e546f6b656e496e666f53746f726506546f6b656e730855736572496e666f0b56696f6c61734576656e7406617070656e6406626f72726f770a626f72726f775f6d757405656d707479066c656e67746809707573685f6261636b08746f5f62797465730b4576656e7448616e646c650a656d69745f6576656e74106e65775f6576656e745f68616e646c650762616c616e636510636f6e74726163745f616464726573730c6372656174655f746f6b656e076465706f7369740b656d69745f6576656e747312657874656e645f757365725f746f6b656e73046a6f696e056a6f696e32046d696e740a6d6f76655f6f776e65720f7061795f66726f6d5f73656e646572077075626c6973680d726571756972655f6f776e657211726571756972655f7075626c697368656412726571756972655f73757065727669736f720573706c69740b746f6b656e5f636f756e740c746f74616c5f737570706c79087472616e736665720576616c7565087769746864726177047a65726f01740e706565725f746f6b656e5f69647811706565725f746f6b656e5f616d6f756e740b64756d6d795f6669656c6405696e646578056f776e657204646174610e62756c6c6574696e5f66697273740962756c6c6574696e7306746f6b656e730274730d76696f6c61735f6576656e7473066f72646572730f6f726465725f66726565736c6f74730565747970650570617261737257c2417e4d1038e1817c8f283ace2e000000000000000000000000000000000002032c08022d032e030102012f010202023003290303020531052703320a02330a02340a0a02040201350a0803050201360a0802060204370b08010807320a02380a0800390a030702033a033b0a02320a0209010105ffff031a1a002c2f050c030a000a0310003e0024030a000514000b0310000a003e010c010b011001150c020518000b03010600000000000000000c020b02020a0000ffff030102000700020b01020406ffff031b200012161217120a2e040c030a0310023e020c020b030f020a000600000000000000000e01153e033e0413033e050e003e060c040d040b013e070601000000000000000b040e023e08120d0a02020c01020405ffff031e17000a00120e0b0114020c050c020a002e050c040b040f000a023e090c030a031001150a05170b030f0116020d000106ffff031f0b002c2e060c030b030f030a000b010b0213073e0a020e00020405ffff03212200120a2e040c020b0210023e020c010a002e050c030a0310003e000c040a040a0127031400051700020b03010513000a030f000a0406000000000000000013023e0b0a04060100000000000000170c04050e000f0100ffff032218000b0014020c060c020b0114020c070c030a020a03220c040b04030f0005100005120006ca00000000000000280a020a060a0717130202100100ffff03231b000b0114020c050c020a001004150a02220c030b03030d00050e000512000b000106ca00000000000000280a001001150a05170b000f011602110103040506ffff03242d0012160a0012150a000a0213020c040a010b04120c120a2e040c060b060f020a003e0c0c050a051005150a02170b050f05160e003e080c070d070e013e063e070d070e023e083e070d070b033e070602000000000000000b073e03120d021201020406ffff03251e0012160a001215120a2e040c040b040f020a003e0c0c030a010b030f06160e003e080c050d050e013e063e070d050b023e070607000000000000000b053e03120d021300020405ffff032608000a000a02121d0c030a010b03120c0214010106ffff032726002c0c010a012d05210c020b02030900050a00050c00066a00000000000000283e0d130531053e0e0e00153e0f3e10130631060a01120a22031b0005210009130131013e11130431040600000000000000000b003e03120d0215000104ffff03292500120a2e040c070a0710023e020c010a000a01240c020b02030e00050f000513000b0701066700000000000000280b0710020a003e120c060b061006152c220c040b040321000522000524000668000000000000002802160000ffff032a0a002c2d050c000b000306000507000509000665000000000000002802170000ffff032a0a002c2d010c000b000306000507000509000666000000000000002802180100ffff032b1c000a001001150a01270c020b02030900050a00050e000b000106cb00000000000000280a001001150a01180a000f01160b001004150a0113020219010104ffff032c0700120a2f040c000b0010023e02021a010104ffff032d0c00120a2f040c020b0210020a003e120c010b01100515021b0103040506ffff032e180012160a000a010a0212130e003e080c040d040e013e063e070d040e023e083e070d040b033e070603000000000000000b043e03120d021c0100ffff031804000b00100115021d010105ffff032f22002c2e050c050b050f000a003e090c020a021001150a01270c030b030311000512000516000b0201066900000000000000280a021001150a01180b020f01160a000a011302021e0100ffff030804000a000600000000000000001302020500020104000600020003010300"

        private const val mCreateTokenContract =
            "a11ceb0b010006013d0000000400000003410000000a000000054b00000007000000075200000025000000067700000010000000098700000011000000000000010102000100000300020002050a02010300063c53454c463e0b56696f6c6173546f6b656e0c6372656174655f746f6b656e046d61696e7257c2417e4d1038e1817c8f283ace2e010000ffff030005000a000b0112000102"
    }

    fun getBalance(tokenIndex: Long): Long {
        return multiContractRpcApi?.getBalance(tokenIndex) ?: 0
    }

    /**
     * 创建 Token 转账 payload
     */
    fun optionTokenTransactionPayload(
        tokenIdx: Long,
        address: String,
        amount: Long,
        data: ByteArray
    ): TransactionPayload {
        val moveEncode =
            Move.violasReplaceAddress(mTransferContract.hexToBytes(), contractAddress.hexToBytes())

        val tokenIdxArgument = TransactionArgument.newU64(tokenIdx)
        val addressArgument = TransactionArgument.newAddress(address)
        val amountArgument = TransactionArgument.newU64(amount)
        val dataArgument = TransactionArgument.newByteArray(data)

        return TransactionPayload(
            TransactionPayload.Script(
                moveEncode,
                arrayListOf(),
                arrayListOf(tokenIdxArgument, addressArgument, amountArgument, dataArgument)
            )
        )
    }

    /**
     * 注册 Token 交易 payload
     */
    fun optionPublishTransactionPayload(
        data: ByteArray = byteArrayOf()
    ): TransactionPayload {
        val moveEncode =
            Move.violasReplaceAddress(mPublishContract.hexToBytes(), contractAddress.hexToBytes())

        val dataArgument = TransactionArgument.newByteArray(data)

        return TransactionPayload(
            TransactionPayload.Script(
                moveEncode,
                arrayListOf(),
                arrayListOf(dataArgument)
            )
        )
    }

    /**
     * 铸造 Token 交易 payload
     */
    fun optionMintTransactionPayload(
        tokenIdx: Long,
        address: String,
        amount: Long,
        data: ByteArray
    ): TransactionPayload {
        val moveEncode =
            Move.violasReplaceAddress(mMintContract.hexToBytes(), contractAddress.hexToBytes())

        val tokenIdxArgument = TransactionArgument.newU64(tokenIdx)
        val addressArgument = TransactionArgument.newAddress(address)
        val amountArgument = TransactionArgument.newU64(amount)
        val dataArgument = TransactionArgument.newByteArray(data)

        return TransactionPayload(
            TransactionPayload.Script(
                moveEncode,
                arrayListOf(lbrStructTag()),
                arrayListOf(tokenIdxArgument, addressArgument, amountArgument, dataArgument)
            )
        )
    }

    /**
     * 生成链上注册稳定比交易 Payload
     *
     * @param tokenAddress 稳定币的 model address
     */
    fun optionReleaseTokenPayload(): TransactionPayload {
        val moveEncode =
            Move.violasMultiReplaceAddress(
                mTokenContract.hexToBytes(),
                contractAddress.hexToBytes()
            )

        return TransactionPayload(
            TransactionPayload.Module(
                moveEncode
            )
        )
    }

    /**
     * 生成链上注册稳定比交易 Payload
     *
     * @param tokenAddress 稳定币的 model address
     */
    fun optionCreateTokenPayload(owner: String, tokenData: ByteArray): TransactionPayload {
        val moveEncode =
            Move.violasReplaceAddress(
                mCreateTokenContract.hexToBytes(),
                contractAddress.hexToBytes()
            )

        val addressArgument = TransactionArgument.newAddress(owner)
        val tokenDataArgument = TransactionArgument.newByteArray(tokenData)

        return TransactionPayload(
            TransactionPayload.Script(
                moveEncode,
                arrayListOf(lbrStructTag()),
                arrayListOf(addressArgument, tokenDataArgument)
            )
        )
    }
}