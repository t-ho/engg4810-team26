/*
 * adc.c
 *
 *  Created on: 12 May 2016
 *      Author: Ryan
 */

#include "adc.h"

uint16_t adc_pos = 0;
uint16_t adc_buffer[ADC_BUF_SIZE] __attribute__(( aligned(8) ));

static uint32_t udmaCtrlTable[1024/sizeof(uint32_t)] __attribute__(( aligned(1024) ));

void
ADC_Init(void)
{
    memset(&adc_buffer, 0, sizeof(adc_buffer));

    SysCtlPeripheralEnable(SYSCTL_PERIPH_ADC0);
    SysCtlPeripheralEnable(SYSCTL_PERIPH_UDMA);
    while (!SysCtlPeripheralReady(SYSCTL_PERIPH_ADC0));
    while (!SysCtlPeripheralReady(SYSCTL_PERIPH_UDMA));

    ADCClockConfigSet(ADC0_BASE, ADC_CLOCK_SRC_PLL | ADC_CLOCK_RATE_FULL, 16);

//    ADCSequenceConfigure(ADC0_BASE, 0 /*SS0*/, ADC_TRIGGER_PROCESSOR, 3 /*priority*/);  // SS0-SS3 priorities must always be different
//    ADCSequenceConfigure(ADC0_BASE, 3 /*SS3*/, ADC_TRIGGER_PROCESSOR, 0 /*priority*/);  // so change SS3 to prio0 when SS0 gets set to prio3

    ADCSequenceConfigure(ADC0_BASE, 0, ADC_TRIGGER_ALWAYS, 0);
//    ADCSequenceConfigure(ADC0_BASE, 0, ADC_TRIGGER_PROCESSOR, 0);
    // Pin set to AIN0 (PE3)
    int i;

    for (i = 0; i < 7; i++)
    {
        ADCSequenceStepConfigure(ADC0_BASE, 0, i, ADC_CTL_CH0);
    }
    ADCSequenceStepConfigure(ADC0_BASE, 0, 7,  ADC_CTL_CH0 | ADC_CTL_IE | ADC_CTL_END);

    ADCSequenceEnable(ADC0_BASE, 0);

    uDMAEnable();
    uDMAControlBaseSet(udmaCtrlTable);
    ADCSequenceDMAEnable(ADC0_BASE, 0);

    // disable some bits
    uDMAChannelAttributeDisable(UDMA_CHANNEL_ADC0, UDMA_ATTR_ALTSELECT /*start with ping-pong PRI side*/ |
         UDMA_ATTR_REQMASK /*unmask*/);
    // enable some bits
    uDMAChannelAttributeEnable(UDMA_CHANNEL_ADC0, UDMA_ATTR_USEBURST /*only allow burst transfers*/ | UDMA_ATTR_HIGH_PRIORITY /*low priority*/);
    // set dma params on PRI_ and ALT_SELECT
    uDMAChannelControlSet(UDMA_CHANNEL_ADC0 | UDMA_PRI_SELECT, UDMA_SIZE_16 | UDMA_SRC_INC_NONE | UDMA_DST_INC_16 | UDMA_ARB_512);
    uDMAChannelControlSet(UDMA_CHANNEL_ADC0 | UDMA_ALT_SELECT, UDMA_SIZE_16 | UDMA_SRC_INC_NONE | UDMA_DST_INC_16 | UDMA_ARB_512);


    uDMAChannelTransferSet(UDMA_CHANNEL_ADC0 | UDMA_PRI_SELECT, UDMA_MODE_PINGPONG,
    		(void *)(ADC0_BASE + ADC_O_SSFIFO0), adc_buffer, ADC_SAMPLE_BUF_SIZE);
    uDMAChannelTransferSet(UDMA_CHANNEL_ADC0 | UDMA_ALT_SELECT, UDMA_MODE_PINGPONG,
    		(void *)(ADC0_BASE + ADC_O_SSFIFO0), adc_buffer, ADC_SAMPLE_BUF_SIZE);

	//ADCIntEnableEx(ADC0_BASE, ADC_INT_DMA_SS0);
	uDMAChannelEnable(UDMA_CHANNEL_ADC0);

	//ADCIntRegister(ADC0_BASE, 0, &adcDmaCallback);
	ADCIntEnable(ADC0_BASE, 0);
}

static void
ADCprocess(uint32_t ch)
{
	//if (uDMAChannelModeGet(ch) != UDMA_MODE_STOP) return;
	// Faster way?
    if ((((tDMAControlTable *) udmaCtrlTable)[ch].ui32Control & UDMA_CHCTL_XFERMODE_M) != UDMA_MODE_STOP) return;

    // store the next buffer in the uDMA transfer descriptor
    // the ADC is read directly into the correct emacBufTx to be transmitted
    uDMAChannelTransferSet(ch, UDMA_MODE_PINGPONG, (void *)(ADC0_BASE + ADC_O_SSFIFO0), &adc_buffer[adc_pos], ADC_SAMPLE_BUF_SIZE);
}

void
adcDmaCallback(unsigned int arg)
{
	ADCIntClear(ADC0_BASE, 0);

	adc_pos += ADC_SAMPLE_BUF_SIZE;

    if (adc_pos >= ADC_BUF_SIZE)
    {
    	adc_pos = 0;
    }

    ADCprocess(UDMA_CHANNEL_ADC0 | UDMA_PRI_SELECT);
    ADCprocess(UDMA_CHANNEL_ADC0 | UDMA_ALT_SELECT);

    uDMAChannelEnable(UDMA_CHANNEL_ADC0);
}