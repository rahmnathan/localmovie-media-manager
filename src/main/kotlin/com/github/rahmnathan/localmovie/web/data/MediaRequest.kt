package com.github.rahmnathan.localmovie.web.data

import com.github.rahmnathan.localmovie.data.MediaClient
import com.github.rahmnathan.localmovie.data.MediaOrder

data class MediaRequest(val path: String, val page: Int, val resultsPerPage: Int, val client: MediaClient?,
                        val order: MediaOrder = MediaOrder.TITLE)
