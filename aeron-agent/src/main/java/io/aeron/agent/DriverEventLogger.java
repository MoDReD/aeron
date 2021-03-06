/*
 * Copyright 2014-2020 Real Logic Limited.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.aeron.agent;

import org.agrona.DirectBuffer;
import org.agrona.concurrent.UnsafeBuffer;
import org.agrona.concurrent.ringbuffer.RingBuffer;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;

import static io.aeron.agent.CommonEventEncoder.encode;
import static io.aeron.agent.CommonEventEncoder.*;
import static io.aeron.agent.DriverEventCode.*;
import static io.aeron.agent.DriverEventEncoder.encode;
import static io.aeron.agent.DriverEventEncoder.*;
import static io.aeron.agent.EventConfiguration.DRIVER_EVENT_CODES;
import static io.aeron.agent.EventConfiguration.EVENT_RING_BUFFER;
import static org.agrona.BitUtil.SIZE_OF_INT;
import static org.agrona.BitUtil.SIZE_OF_LONG;

/**
 * Event logger interface used by interceptors for recording into a {@link RingBuffer} for a
 * {@link io.aeron.driver.MediaDriver} via a Java Agent.
 */
public final class DriverEventLogger
{
    public static final DriverEventLogger LOGGER = new DriverEventLogger(EVENT_RING_BUFFER);

    private final RingBuffer ringBuffer;

    DriverEventLogger(final RingBuffer ringBuffer)
    {
        this.ringBuffer = ringBuffer;
    }

    public void log(final DriverEventCode code, final DirectBuffer srcBuffer, final int srcOffset, final int length)
    {
        if (DRIVER_EVENT_CODES.contains(code))
        {
            final int captureLength = captureLength(length);
            final int encodedLength = encodedLength(captureLength);

            final int index = ringBuffer.tryClaim(toEventCodeId(code), encodedLength);
            if (index > 0)
            {
                try
                {
                    encode((UnsafeBuffer)ringBuffer.buffer(), index, captureLength, length, srcBuffer, srcOffset);
                }
                finally
                {
                    ringBuffer.commit(index);
                }
            }
        }
    }

    public void logFrameIn(
        final DirectBuffer srcBuffer, final int srcOffset, final int bufferLength, final InetSocketAddress dstAddress)
    {
        final int length = bufferLength + socketAddressLength(dstAddress);
        final int captureLength = captureLength(length);
        final int encodedLength = encodedLength(captureLength);

        final int index = ringBuffer.tryClaim(toEventCodeId(FRAME_IN), encodedLength);
        if (index > 0)
        {
            try
            {
                encode(
                    (UnsafeBuffer)ringBuffer.buffer(), index, captureLength, length, srcBuffer, srcOffset, dstAddress);
            }
            finally
            {
                ringBuffer.commit(index);
            }
        }
    }

    public void logFrameOut(final ByteBuffer srcBuffer, final InetSocketAddress dstAddress)
    {
        final int length = srcBuffer.remaining() + socketAddressLength(dstAddress);
        final int captureLength = captureLength(length);
        final int encodedLength = encodedLength(captureLength);

        final int index = ringBuffer.tryClaim(toEventCodeId(FRAME_OUT), encodedLength);
        if (index > 0)
        {
            try
            {
                encode(
                    (UnsafeBuffer)ringBuffer.buffer(),
                    index,
                    captureLength,
                    length,
                    srcBuffer,
                    srcBuffer.position(),
                    dstAddress);
            }
            finally
            {
                ringBuffer.commit(index);
            }
        }
    }

    public void logPublicationRemoval(final String uri, final int sessionId, final int streamId)
    {
        final int length = SIZE_OF_INT * 3 + uri.length();
        final int captureLength = captureLength(length);
        final int encodedLength = encodedLength(captureLength);

        final int index = ringBuffer.tryClaim(toEventCodeId(REMOVE_PUBLICATION_CLEANUP), encodedLength);
        if (index > 0)
        {
            try
            {
                encodePublicationRemoval(
                    (UnsafeBuffer)ringBuffer.buffer(), index, captureLength, length, uri, sessionId, streamId);
            }
            finally
            {
                ringBuffer.commit(index);
            }
        }
    }

    public void logSubscriptionRemoval(final String uri, final int streamId, final long id)
    {
        final int length = SIZE_OF_INT * 2 + SIZE_OF_LONG + uri.length();
        final int captureLength = captureLength(length);
        final int encodedLength = encodedLength(captureLength);

        final int index = ringBuffer.tryClaim(toEventCodeId(REMOVE_SUBSCRIPTION_CLEANUP), encodedLength);
        if (index > 0)
        {
            try
            {
                encodeSubscriptionRemoval(
                    (UnsafeBuffer)ringBuffer.buffer(), index, captureLength, length, uri, streamId, id);
            }
            finally
            {
                ringBuffer.commit(index);
            }
        }
    }

    public void logImageRemoval(final String uri, final int sessionId, final int streamId, final long id)
    {
        final int length = SIZE_OF_INT * 3 + SIZE_OF_LONG + uri.length();
        final int captureLength = captureLength(length);
        final int encodedLength = encodedLength(captureLength);

        final int index = ringBuffer.tryClaim(toEventCodeId(REMOVE_IMAGE_CLEANUP), encodedLength);
        if (index > 0)
        {
            try
            {
                encodeImageRemoval(
                    (UnsafeBuffer)ringBuffer.buffer(), index, captureLength, length, uri, sessionId, streamId, id);
            }
            finally
            {
                ringBuffer.commit(index);
            }
        }
    }

    public void logString(final DriverEventCode code, final String value)
    {
        final int length = value.length() + SIZE_OF_INT;
        final int captureLength = captureLength(length);
        final int encodedLength = encodedLength(captureLength);

        final int index = ringBuffer.tryClaim(toEventCodeId(code), encodedLength);
        if (index > 0)
        {
            try
            {
                encode((UnsafeBuffer)ringBuffer.buffer(), index, captureLength, length, value);
            }
            finally
            {
                ringBuffer.commit(index);
            }
        }
    }

    public static int toEventCodeId(final DriverEventCode code)
    {
        return EVENT_CODE_TYPE << 16 | (code.id() & 0xFFFF);
    }
}
