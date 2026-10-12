package com.farmingnavigator.transport;

public class VerifiedTransportResult
{
    private final TransportOption option;
    private final VerifiedTransportState state;
    private final String detail;

    public VerifiedTransportResult(
            TransportOption option,
            VerifiedTransportState state,
            String detail
    )
    {
        this.option = option;
        this.state = state;
        this.detail = detail;
    }

    public TransportOption getOption()
    {
        return option;
    }

    public VerifiedTransportState getState()
    {
        return state;
    }

    public String getDetail()
    {
        return detail;
    }

    public boolean isUsableForPlannedRun()
    {
        return state
                == VerifiedTransportState.READY_NOW
                || state
                == VerifiedTransportState.AVAILABLE_FROM_BANK;
    }
}