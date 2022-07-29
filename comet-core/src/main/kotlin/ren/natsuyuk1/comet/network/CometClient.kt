/*
 * Copyright (c) 2019-2022 StarWishsama.
 *
 * 此源代码的使用受 GNU General Affero Public License v3.0 许可证约束, 欲阅读此许可证, 可在以下链接查看.
 * Use of this source code is governed by the GNU AGPLv3 license which can be found through the following link.
 *
 * https://github.com/StarWishsama/Comet-Bot/blob/master/LICENSE
 */

package ren.natsuyuk1.comet.network

import io.ktor.client.*
import kotlinx.coroutines.CoroutineName
import ren.natsuyuk1.comet.consts.defaultClient
import ren.natsuyuk1.comet.network.thirdparty.bilibili.initYabapi
import kotlin.coroutines.CoroutineContext

class CometClient(
    var client: HttpClient = defaultClient,
    val context: CoroutineContext = CoroutineName("comet-client")
) {
    init {
        initYabapi()
    }
}