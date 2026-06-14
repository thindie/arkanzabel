package com.v2ray.ang.util

import com.v2ray.ang.dto.ConnectionProfile

interface ConnectionProfileSummariser {
   fun isSavedAsFavorite(connectionProfile: ConnectionProfile): Boolean
}