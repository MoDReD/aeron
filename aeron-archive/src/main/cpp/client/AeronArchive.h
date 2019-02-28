/*
 * Copyright 2014-2019 Real Logic Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
#ifndef AERON_ARCHIVE_AERONARCHIVE_H
#define AERON_ARCHIVE_AERONARCHIVE_H

#include "Aeron.h"
#include "ArchiveConfiguration.h"
#include "ControlResponsePoller.h"
#include "concurrent/BackOffIdleStrategy.h"
#include "concurrent/YieldingIdleStrategy.h"

namespace aeron {
namespace archive {
namespace client {

class AeronArchive
{
public:
    using Context_t = aeron::archive::client::Context;

    AeronArchive(Context_t& context);
    ~AeronArchive();

    class AsyncConnect
    {
    public:
        AsyncConnect(Context_t& context);
        ~AsyncConnect();

        std::shared_ptr<AeronArchive> poll();
    private:
        Context_t m_ctx;
    };

    static std::shared_ptr<AsyncConnect> asyncConnect(Context_t& context);

    inline static std::shared_ptr<AsyncConnect> asyncConnect()
    {
        Context_t ctx;
        return AeronArchive::asyncConnect(ctx);
    }

    template<typename ConnectIdleStrategy = aeron::concurrent::YieldingIdleStrategy>
    inline static std::shared_ptr<AeronArchive> connect(Context_t& context)
    {
        std::shared_ptr<AsyncConnect> asyncConnect = AeronArchive::asyncConnect(context);
        ConnectIdleStrategy idle;

        std::shared_ptr<AeronArchive> archive = asyncConnect->poll();
        while (!archive)
        {
            idle.idle();
            archive = asyncConnect->poll();
        }

        return archive;
    }

    inline static std::shared_ptr<AeronArchive> connect()
    {
        Context_t ctx;
        return AeronArchive::connect(ctx);
    }

    template<typename IdleStrategy = aeron::concurrent::BackoffIdleStrategy>
    std::int64_t startReplay(
        std::int64_t recordingId,
        std::int64_t position,
        std::int64_t length,
        const std::string& replayChannel,
        std::int32_t replayStreamId)
    {
        const std::int64_t correlationId = m_aeron->nextCorrelationId();

        // TODO: finish

        return pollForResponse<IdleStrategy>(correlationId);
    }

private:
    std::shared_ptr<Aeron> m_aeron;
    Context_t m_ctx;

    template<typename IdleStrategy>
    int pollForResponse(std::int64_t correlationId)
    {
        IdleStrategy idle;

        // TODO: finish

        return 0;
    }
};

}}}
#endif //AERON_ARCHIVE_AERONARCHIVE_H