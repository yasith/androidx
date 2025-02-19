/*
 * Copyright 2021 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.wear.compose.foundation.samples

import androidx.annotation.Sampled
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.foundation.ArcPaddingValues
import androidx.wear.compose.foundation.BasicCurvedText
import androidx.wear.compose.foundation.CurvedRow
import androidx.wear.compose.foundation.CurvedTextStyle

@Sampled
@Composable
fun SimpleCurvedRow() {
    CurvedRow(modifier = Modifier.fillMaxSize()) {
        BasicText(
            "Simple",
            Modifier.background(Color.White).padding(2.dp),
            TextStyle(
                color = Color.Black,
                fontSize = 16.sp,
            )
        )
        Box(
            modifier = Modifier
                .size(20.dp)
                .background(Color.Gray)
        )
        BasicText(
            "CurvedRow",
            Modifier.background(Color.White).padding(2.dp),
            TextStyle(
                color = Color.Black,
                fontSize = 16.sp,
            )
        )
    }
}

@Sampled
@Composable
fun CurvedAndNormalText() {
    CurvedRow(modifier = Modifier.fillMaxSize()) {
        BasicCurvedText(
            "Curved Text",
            CurvedTextStyle(
                fontSize = 16.sp,
                color = Color.Black,
                background = Color.White
            ),
            contentArcPadding = ArcPaddingValues(2.dp)
        )
        Box(
            modifier = Modifier
                .size(20.dp)
                .background(Color.Gray)
        )
        BasicText(
            "Normal Text",
            Modifier.padding(2.dp),
            TextStyle(
                color = Color.Black,
                fontSize = 16.sp,
                background = Color.White
            )
        )
    }
}
