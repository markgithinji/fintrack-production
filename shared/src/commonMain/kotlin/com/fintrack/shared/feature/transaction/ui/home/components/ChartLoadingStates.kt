package com.fintrack.shared.feature.transaction.ui.home.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.fintrack.shared.feature.core.ui.AnimatedShimmerBox

@Composable
fun LoadingBarChart() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(240.dp)
            .padding(top = 4.dp, end = 16.dp)
    ) {
        // Y-axis placeholder
        Row(
            modifier = Modifier.fillMaxWidth().height(180.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            // Y-axis labels
            Column(
                modifier = Modifier
                    .width(28.dp)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                repeat(5) {
                    AnimatedShimmerBox(
                        modifier = Modifier
                            .width(24.dp)
                            .height(10.dp)
                            .clip(RoundedCornerShape(4.dp))
                    )
                }
            }

            Spacer(Modifier.width(8.dp))

            // Bars
            Row(
                modifier = Modifier.weight(1f).fillMaxHeight(),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.Bottom
            ) {
                repeat(7) {
                    AnimatedShimmerBox(
                        modifier = Modifier
                            .width(16.dp)
                            .height(180.dp)
                            .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // X-axis labels shimmers
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 36.dp),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            repeat(7) {
                AnimatedShimmerBox(
                    modifier = Modifier
                        .width(20.dp)
                        .height(10.dp)
                        .clip(RoundedCornerShape(4.dp))
                )
            }
        }
    }
}

@Composable
fun LoadingLineChart() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(240.dp)
            .padding(top = 4.dp, end = 16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().height(180.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            // Y-axis labels
            Column(
                modifier = Modifier
                    .width(28.dp)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                repeat(5) {
                    AnimatedShimmerBox(
                        modifier = Modifier
                            .width(24.dp)
                            .height(10.dp)
                            .clip(RoundedCornerShape(4.dp))
                    )
                }
            }

            Spacer(Modifier.width(8.dp))

            // Line Chart Area Placeholder
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            ) {
                // Background grid lines shimmers
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    repeat(5) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .clip(CircleShape)
                        ) {
                            AnimatedShimmerBox(modifier = Modifier.fillMaxSize())
                        }
                    }
                }

                // Simulated "Line" shimmers
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center
                ) {
                    // Two wavy-ish lines
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.8f)
                            .height(3.dp)
                            .padding(start = 20.dp)
                            .clip(CircleShape)
                    ) {
                        AnimatedShimmerBox(modifier = Modifier.fillMaxSize())
                    }
                    Spacer(modifier = Modifier.height(30.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.6f)
                            .height(3.dp)
                            .padding(start = 50.dp)
                            .clip(CircleShape)
                    ) {
                        AnimatedShimmerBox(modifier = Modifier.fillMaxSize())
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // X-axis labels shimmers
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 36.dp),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            repeat(7) {
                AnimatedShimmerBox(
                    modifier = Modifier
                        .width(20.dp)
                        .height(10.dp)
                        .clip(RoundedCornerShape(4.dp))
                )
            }
        }
    }
}
