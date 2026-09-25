<?xml version="1.0" encoding="UTF-8" standalone="no"?>
<goldensource-package version="8.7.1.106">
<package-comment/>
<businessobject displayString="1 - 8.7.1.88_VNRDSEv50" type="com.j2fe.workflow.definition.Workflow">
<com.j2fe.workflow.definition.Workflow id="0">
<alwaysPersist>false</alwaysPersist>
<clustered>false</clustered>
<comment id="1">8.7.1.88_VNRDSEv50</comment>
<endNode id="2">
<activation>INVOKE</activation>
<clusteredCall>false</clusteredCall>
<directJoin>false</directJoin>
<name id="3">END</name>
<nodeHandler>com.j2fe.workflow.handler.impl.DummyActivityHandler</nodeHandler>
<nodeHandlerClass id="4">com.j2fe.workflow.handler.impl.DummyActivityHandler</nodeHandlerClass>
<sources id="5" type="java.util.HashSet">
<item id="6" type="com.j2fe.workflow.definition.Transition">
<name id="7">goto-next</name>
<source id="8">
<activation>INVOKE</activation>
<clusteredCall>false</clusteredCall>
<directJoin>false</directJoin>
<name id="9">NOP</name>
<nodeHandler>com.j2fe.workflow.handler.impl.DummyActivityHandler</nodeHandler>
<nodeHandlerClass id="10">com.j2fe.workflow.handler.impl.DummyActivityHandler</nodeHandlerClass>
<sources id="11" type="java.util.HashSet">
<item id="12" type="com.j2fe.workflow.definition.Transition">
<name id="13">empty</name>
<source id="14">
<activation>INVOKE</activation>
<clusteredCall>false</clusteredCall>
<directJoin>false</directJoin>
<name id="15">Array Helper</name>
<nodeHandler>com.j2fe.general.activities.ArrayHelper</nodeHandler>
<nodeHandlerClass id="16">com.j2fe.general.activities.ArrayHelper</nodeHandlerClass>
<parameters id="17" type="java.util.HashSet">
<item id="18" type="com.j2fe.workflow.definition.Parameter">
<input>true</input>
<name id="19">array</name>
<stringValue id="20">ProcessedEntityInformations</stringValue>
<type>VARIABLE</type>
</item>
</parameters>
<sources id="21" type="java.util.HashSet">
<item id="22" type="com.j2fe.workflow.definition.Transition">
<name id="23">false</name>
<source id="24">
<activation>INVOKE</activation>
<clusteredCall>false</clusteredCall>
<directJoin>false</directJoin>
<name id="25">Is Null</name>
<nodeHandler>com.j2fe.general.activities.IsNull</nodeHandler>
<nodeHandlerClass id="26">com.j2fe.general.activities.IsNull</nodeHandlerClass>
<parameters id="27" type="java.util.HashSet">
<item id="28" type="com.j2fe.workflow.definition.Parameter">
<input>true</input>
<name id="29">input</name>
<stringValue id="30">ProcessedEntityInformations</stringValue>
<type>VARIABLE</type>
</item>
</parameters>
<sources id="31" type="java.util.HashSet">
<item id="32" type="com.j2fe.workflow.definition.Transition">
<name id="33">goto-next</name>
<source id="34">
<activation>INVOKE</activation>
<clusteredCall>false</clusteredCall>
<directJoin>false</directJoin>
<name id="35">Close Transaction</name>
<nodeHandler>com.j2fe.streetlamp.activities.CloseTransaction</nodeHandler>
<nodeHandlerClass id="36">com.j2fe.streetlamp.activities.CloseTransaction</nodeHandlerClass>
<parameters id="37" type="java.util.HashSet">
<item id="38" type="com.j2fe.workflow.definition.Parameter">
<input>true</input>
<name id="39">filtered</name>
<stringValue id="40">false</stringValue>
<type>CONSTANT</type>
</item>
<item id="41" type="com.j2fe.workflow.definition.Parameter">
<input>true</input>
<name id="42">formattedMessages</name>
<stringValue id="43">Translated</stringValue>
<type>VARIABLE</type>
</item>
<item id="44" type="com.j2fe.workflow.definition.Parameter">
<input>false</input>
<name id="45">maxSeverity</name>
<stringValue id="46">Severity</stringValue>
<type>VARIABLE</type>
</item>
<item id="47" type="com.j2fe.workflow.definition.Parameter">
<input>true</input>
<name id="48">messageType</name>
<stringValue id="49">MessageType</stringValue>
<type>VARIABLE</type>
</item>
<item id="50" type="com.j2fe.workflow.definition.Parameter">
<input>true</input>
<name id="51">submittedMessage</name>
<stringValue id="52">Message</stringValue>
<type>VARIABLE</type>
</item>
<item id="53" type="com.j2fe.workflow.definition.Parameter">
<input>true</input>
<name id="54">transactionId</name>
<stringValue id="55">TransactionId</stringValue>
<type>VARIABLE</type>
</item>
<item id="56" type="com.j2fe.workflow.definition.Parameter">
<input>true</input>
<name id="57">transactionMetaData</name>
<stringValue id="58">PublishingTranslatedOutput</stringValue>
<type>VARIABLE</type>
<variablePart id="59">metaData</variablePart>
</item>
<item id="60" type="com.j2fe.workflow.definition.Parameter">
<input>true</input>
<name id="61">translatedTimestamp</name>
<stringValue id="62">TranslatedTimestamp</stringValue>
<type>VARIABLE</type>
</item>
</parameters>
<sources id="63" type="java.util.HashSet">
<item id="64" type="com.j2fe.workflow.definition.Transition">
<name id="65">50</name>
<source id="66">
<activation>INVOKE</activation>
<clusteredCall>false</clusteredCall>
<directJoin>false</directJoin>
<name id="67">Switch Case</name>
<nodeHandler>com.j2fe.workflow.handler.impl.SwitchCaseSplit</nodeHandler>
<nodeHandlerClass id="68">com.j2fe.workflow.handler.impl.SwitchCaseSplit</nodeHandlerClass>
<parameters id="69" type="java.util.HashSet">
<item id="70" type="com.j2fe.workflow.definition.Parameter">
<input>true</input>
<name id="71">caseItem</name>
<stringValue id="72">Severity</stringValue>
<type>VARIABLE</type>
</item>
</parameters>
<sources id="73" type="java.util.HashSet">
<item id="74" type="com.j2fe.workflow.definition.Transition">
<name id="75">ToSplit</name>
<source id="76">
<activation>INVOKE</activation>
<clusteredCall>false</clusteredCall>
<description id="77">Automatically generated</description>
<directJoin>false</directJoin>
<name id="78">Merge</name>
<nodeHandler>com.j2fe.workflow.handler.impl.DummyActivityHandler</nodeHandler>
<nodeHandlerClass id="79">com.j2fe.workflow.handler.impl.DummyActivityHandler</nodeHandlerClass>
<sources id="80" type="java.util.HashSet">
<item id="81" type="com.j2fe.workflow.definition.Transition">
<name id="82">goto-next</name>
<source id="83">
<activation>INVOKE</activation>
<clusteredCall>false</clusteredCall>
<directJoin>false</directJoin>
<name id="84">Process Message</name>
<nodeHandler>com.j2fe.tp.activities.ProcessTransaction</nodeHandler>
<nodeHandlerClass id="85">com.j2fe.tp.activities.ProcessTransaction</nodeHandlerClass>
<parameters id="86" type="java.util.HashSet">
<item id="87" type="com.j2fe.workflow.definition.Parameter">
<input>true</input>
<name id="88">engineResource</name>
<stringValue id="89">engine/TPS-1</stringValue>
<type>REFERENCE</type>
</item>
<item id="90" type="com.j2fe.workflow.definition.Parameter">
<input>true</input>
<name id="91">inputFromTranslation</name>
<stringValue id="92">PublishingTranslatedOutput</stringValue>
<type>VARIABLE</type>
</item>
<item id="93" type="com.j2fe.workflow.definition.Parameter">
<input>true</input>
<name id="94">messageMetaData</name>
<stringValue id="95">MessageMetaData</stringValue>
<type>VARIABLE</type>
</item>
<item id="96" type="com.j2fe.workflow.definition.Parameter">
<input>true</input>
<name id="97">messageType</name>
<stringValue id="98">MessageType</stringValue>
<type>VARIABLE</type>
</item>
<item id="99" type="com.j2fe.workflow.definition.Parameter">
<input>false</input>
<name id="100">processed</name>
<stringValue id="101">Processed</stringValue>
<type>VARIABLE</type>
</item>
<item id="102" type="com.j2fe.workflow.definition.Parameter">
<input>false</input>
<name id="103">processedEntityInfo</name>
<stringValue id="104">ProcessedEntityInformations</stringValue>
<type>VARIABLE</type>
</item>
<item id="105" type="com.j2fe.workflow.definition.Parameter">
<input>false</input>
<name id="106">severity.severity</name>
<stringValue id="107">Severity</stringValue>
<type>VARIABLE</type>
</item>
<item id="108" type="com.j2fe.workflow.definition.Parameter">
<input>true</input>
<name id="109">transactionId</name>
<stringValue id="110">TransactionId</stringValue>
<type>VARIABLE</type>
</item>
</parameters>
<sources id="111" type="java.util.HashSet">
<item id="112" type="com.j2fe.workflow.definition.Transition">
<name id="113">false</name>
<source id="114">
<activation>INVOKE</activation>
<clusteredCall>false</clusteredCall>
<directJoin>false</directJoin>
<name id="115">Switch Case</name>
<nodeHandler>com.j2fe.workflow.handler.impl.SwitchCaseSplit</nodeHandler>
<nodeHandlerClass id="116">com.j2fe.workflow.handler.impl.SwitchCaseSplit</nodeHandlerClass>
<parameters id="117" type="java.util.HashSet">
<item id="118" type="com.j2fe.workflow.definition.Parameter">
<input>true</input>
<name id="119">caseItem</name>
<stringValue id="120">IsWorkstationMessage</stringValue>
<type>VARIABLE</type>
</item>
<item id="121" type="com.j2fe.workflow.definition.Parameter">
<input>true</input>
<name id="122">nullTransition</name>
<stringValue id="123">false</stringValue>
<type>CONSTANT</type>
</item>
</parameters>
<sources id="124" type="java.util.HashSet">
<item id="125" type="com.j2fe.workflow.definition.Transition">
<name id="126">ToSplit</name>
<source id="127">
<activation>INVOKE</activation>
<clusteredCall>false</clusteredCall>
<description id="128">Automatically generated</description>
<directJoin>false</directJoin>
<name id="129">Merge</name>
<nodeHandler>com.j2fe.workflow.handler.impl.DummyActivityHandler</nodeHandler>
<nodeHandlerClass id="130">com.j2fe.workflow.handler.impl.DummyActivityHandler</nodeHandlerClass>
<sources id="131" type="java.util.HashSet">
<item id="132" type="com.j2fe.workflow.definition.Transition">
<name id="133">false</name>
<source id="134">
<activation>INVOKE</activation>
<clusteredCall>false</clusteredCall>
<directJoin>false</directJoin>
<name id="135">Filter Message?</name>
<nodeHandler>com.j2fe.workflow.handler.impl.SwitchCaseSplit</nodeHandler>
<nodeHandlerClass id="136">com.j2fe.workflow.handler.impl.SwitchCaseSplit</nodeHandlerClass>
<parameters id="137" type="java.util.HashSet">
<item id="138" type="com.j2fe.workflow.definition.Parameter">
<input>true</input>
<name id="139">caseItem</name>
<stringValue id="140">PublishingTranslatedOutput</stringValue>
<type>VARIABLE</type>
<variablePart id="141">filteredFromGSDM</variablePart>
</item>
<item id="142" type="com.j2fe.workflow.definition.Parameter">
<input>true</input>
<name id="143">defaultItem</name>
<stringValue id="144">false</stringValue>
<type>CONSTANT</type>
</item>
<item id="145" type="com.j2fe.workflow.definition.Parameter">
<input>true</input>
<name id="146">nullTransition</name>
<stringValue id="147">false</stringValue>
<type>CONSTANT</type>
</item>
</parameters>
<sources id="148" type="java.util.HashSet">
<item id="149" type="com.j2fe.workflow.definition.Transition">
<name id="150">not-empty</name>
<source id="151">
<activation>INVOKE</activation>
<clusteredCall>false</clusteredCall>
<directJoin>false</directJoin>
<name id="152">Translate Message</name>
<nodeHandler>com.j2fe.translation.activities.Translation</nodeHandler>
<nodeHandlerClass id="153">com.j2fe.translation.activities.Translation</nodeHandlerClass>
<parameters id="154" type="java.util.HashSet">
<item id="155" type="com.j2fe.workflow.definition.Parameter">
<input>true</input>
<name id="156">MSFDBSelect</name>
<stringValue id="157">jdbc/GSDM-1</stringValue>
<type>REFERENCE</type>
</item>
<item id="158" type="com.j2fe.workflow.definition.Parameter">
<input>true</input>
<name id="159">input</name>
<stringValue id="160">Message</stringValue>
<type>VARIABLE</type>
</item>
<item id="161" type="com.j2fe.workflow.definition.Parameter">
<input>true</input>
<name id="162">messageType</name>
<stringValue id="163">MessageType</stringValue>
<type>VARIABLE</type>
</item>
<item id="164" type="com.j2fe.workflow.definition.Parameter">
<input>true</input>
<name id="165">metaData</name>
<stringValue id="166">MessageMetaData</stringValue>
<type>VARIABLE</type>
</item>
<item id="167" type="com.j2fe.workflow.definition.Parameter">
<input>false</input>
<name id="168">output</name>
<stringValue id="169">PublishingTranslatedOutput</stringValue>
<type>VARIABLE</type>
</item>
<item id="170" type="com.j2fe.workflow.definition.Parameter">
<input>false</input>
<name id="171">plainOutput</name>
<stringValue id="172">Translated</stringValue>
<type>VARIABLE</type>
</item>
<item id="173" type="com.j2fe.workflow.definition.Parameter">
<input>true</input>
<name id="174">syncWriteUINotification</name>
<stringValue id="175">IsWorkstationMessage</stringValue>
<type>VARIABLE</type>
</item>
<item id="176" type="com.j2fe.workflow.definition.Parameter">
<input>true</input>
<name id="177">transactionId</name>
<stringValue id="178">TransactionId</stringValue>
<type>VARIABLE</type>
</item>
<item id="179" type="com.j2fe.workflow.definition.Parameter">
<input>false</input>
<name id="180">translatedTimestamp</name>
<stringValue id="181">TranslatedTimestamp</stringValue>
<type>VARIABLE</type>
</item>
<item id="182" type="com.j2fe.workflow.definition.Parameter">
<input>true</input>
<name id="183">wantedOutputType</name>
<stringValue id="184">Binary</stringValue>
<type>CONSTANT</type>
</item>
</parameters>
<sources id="185" type="java.util.HashSet">
<item id="186" type="com.j2fe.workflow.definition.Transition">
<name id="187">false</name>
<source id="188">
<activation>INVOKE</activation>
<clusteredCall>false</clusteredCall>
<directJoin>false</directJoin>
<name id="189">isArray of messages</name>
<nodeHandler>com.j2fe.workflow.handler.impl.SwitchCaseSplit</nodeHandler>
<nodeHandlerClass id="190">com.j2fe.workflow.handler.impl.SwitchCaseSplit</nodeHandlerClass>
<parameters id="191" type="java.util.HashSet">
<item id="192" type="com.j2fe.workflow.definition.Parameter">
<input>true</input>
<name id="193">caseItem</name>
<stringValue id="194">messageArray</stringValue>
<type>VARIABLE</type>
</item>
<item id="195" type="com.j2fe.workflow.definition.Parameter">
<input>true</input>
<name id="196">defaultItem</name>
<stringValue id="197">true</stringValue>
<type>CONSTANT</type>
</item>
<item id="198" type="com.j2fe.workflow.definition.Parameter">
<input>true</input>
<name id="199">nullTransition</name>
<stringValue id="200">false</stringValue>
<type>CONSTANT</type>
</item>
</parameters>
<sources id="201" type="java.util.HashSet">
<item id="202" type="com.j2fe.workflow.definition.Transition">
<name id="203">go-to-translation</name>
<source id="204">
<activation>INVOKE</activation>
<clusteredCall>false</clusteredCall>
<directJoin>false</directJoin>
<name id="205">Start</name>
<nodeHandler>com.j2fe.workflow.handler.impl.DummyActivityHandler</nodeHandler>
<nodeHandlerClass id="206">com.j2fe.workflow.handler.impl.DummyActivityHandler</nodeHandlerClass>
<sources id="207" type="java.util.HashSet"/>
<targets id="208" type="java.util.HashSet">
<item idref="202" type="com.j2fe.workflow.definition.Transition"/>
</targets>
<type>START</type>
</source>
<target idref="188"/>
</item>
</sources>
<targets id="209" type="java.util.HashSet">
<item idref="186" type="com.j2fe.workflow.definition.Transition"/>
<item id="210" type="com.j2fe.workflow.definition.Transition">
<name id="211">true</name>
<source idref="188"/>
<target id="212">
<activation>INVOKE</activation>
<clusteredCall>false</clusteredCall>
<description id="213">Automatically generated</description>
<directJoin>false</directJoin>
<name id="214">Merge</name>
<nodeHandler>com.j2fe.workflow.handler.impl.DummyActivityHandler</nodeHandler>
<nodeHandlerClass id="215">com.j2fe.workflow.handler.impl.DummyActivityHandler</nodeHandlerClass>
<sources id="216" type="java.util.HashSet">
<item id="217" type="com.j2fe.workflow.definition.Transition">
<name id="218">goto-next</name>
<source id="219">
<activation>INVOKE</activation>
<clusteredCall>false</clusteredCall>
<directJoin>false</directJoin>
<name id="220">Bean Shell Script (Standard)</name>
<nodeHandler>com.j2fe.general.activities.BeanShellScript</nodeHandler>
<nodeHandlerClass id="221">com.j2fe.general.activities.BeanShellScript</nodeHandlerClass>
<parameters id="222" type="java.util.HashSet">
<item id="223" type="com.j2fe.workflow.definition.Parameter">
<input>true</input>
<name id="224">statements</name>
<stringValue id="225">
tempMessages=new ArrayList() ;

/*
Note: 'resultMessages' represents the global workflow parameter that should contain the final array of messages which needs to be passed to the engine after the loop
*/

if(resultMessages != null)
{
	for(int k=0; k &lt; resultMessages.length; k++)
	{
		tempMessages.add(resultMessages[k]);
	}
}

/*
Note: 'translatedMessage' is the array of the messages returned by the translator activity above
*/

for(int i=0; i &lt; translatedMessage.length; i++)
{
	 tempMessages.add(translatedMessage[i]);
     
}

//taking the firstmessage metadata
if(loopcounter==1)
{
      	firstMsgMetadata=msgMetadata;
}

int mylength = tempMessages.toArray().length;

String[] tempMessagesArray = new String[mylength];

for(int j=0; j &lt; mylength; j++)
{	

   tempMessagesArray[j] = tempMessages.toArray()[j];

}

</stringValue>
<type>CONSTANT</type>
</item>
<item id="226" type="com.j2fe.workflow.definition.Parameter">
<input>false</input>
<name id="227">variables["firstMsgMetadata"]</name>
<stringValue id="228">firstMessageMetaData</stringValue>
<type>VARIABLE</type>
</item>
<item id="229" type="com.j2fe.workflow.definition.Parameter">
<input>true</input>
<name id="230">variables["loopcounter"]</name>
<stringValue id="231">counter</stringValue>
<type>VARIABLE</type>
</item>
<item id="232" type="com.j2fe.workflow.definition.Parameter">
<input>true</input>
<name id="233">variables["msgMetadata"]</name>
<stringValue id="234">Outputmsg</stringValue>
<type>VARIABLE</type>
<variablePart id="235">metaData</variablePart>
</item>
<item id="236" type="com.j2fe.workflow.definition.Parameter">
<input>true</input>
<name id="237">variables["resultMessages"]</name>
<stringValue id="238">TranslatedMessages</stringValue>
<type>VARIABLE</type>
</item>
<item id="239" type="com.j2fe.workflow.definition.Parameter">
<UITypeHint id="240">["tempMessagesArray"]@java/lang/Object@</UITypeHint>
<input>false</input>
<name id="241">variables["tempMessagesArray"]</name>
<stringValue id="242">TranslatedMessages</stringValue>
<type>VARIABLE</type>
</item>
<item id="243" type="com.j2fe.workflow.definition.Parameter">
<input>true</input>
<name id="244">variables["translatedMessage"]</name>
<stringValue id="245">PlainOutput</stringValue>
<type>VARIABLE</type>
</item>
</parameters>
<sources id="246" type="java.util.HashSet">
<item id="247" type="com.j2fe.workflow.definition.Transition">
<name id="248">goto-next</name>
<source id="249">
<activation>INVOKE</activation>
<clusteredCall>false</clusteredCall>
<directJoin>false</directJoin>
<name id="250">Translation (Standard)</name>
<nodeHandler>com.j2fe.translation.activities.Translation</nodeHandler>
<nodeHandlerClass id="251">com.j2fe.translation.activities.Translation</nodeHandlerClass>
<parameters id="252" type="java.util.HashSet">
<item id="253" type="com.j2fe.workflow.definition.Parameter">
<input>true</input>
<name id="254">MSFDBSelect</name>
<stringValue id="255">jdbc/GSDM-1</stringValue>
<type>REFERENCE</type>
</item>
<item id="256" type="com.j2fe.workflow.definition.Parameter">
<input>true</input>
<name id="257">input</name>
<stringValue id="258">Output</stringValue>
<type>VARIABLE</type>
</item>
<item id="259" type="com.j2fe.workflow.definition.Parameter">
<input>true</input>
<name id="260">messageType</name>
<stringValue id="261">MessageType</stringValue>
<type>VARIABLE</type>
</item>
<item id="262" type="com.j2fe.workflow.definition.Parameter">
<input>true</input>
<name id="263">metaData</name>
<stringValue id="264">MessageMetaData</stringValue>
<type>VARIABLE</type>
</item>
<item id="265" type="com.j2fe.workflow.definition.Parameter">
<input>false</input>
<name id="266">output</name>
<stringValue id="267">Outputmsg</stringValue>
<type>VARIABLE</type>
</item>
<item id="268" type="com.j2fe.workflow.definition.Parameter">
<input>false</input>
<name id="269">plainOutput</name>
<stringValue id="270">PlainOutput</stringValue>
<type>VARIABLE</type>
</item>
<item id="271" type="com.j2fe.workflow.definition.Parameter">
<input>true</input>
<name id="272">syncWriteUINotification</name>
<stringValue id="273">IsWorkstationMessage</stringValue>
<type>VARIABLE</type>
</item>
<item id="274" type="com.j2fe.workflow.definition.Parameter">
<input>true</input>
<name id="275">transactionId</name>
<stringValue id="276">TransactionId</stringValue>
<type>VARIABLE</type>
</item>
<item id="277" type="com.j2fe.workflow.definition.Parameter">
<input>false</input>
<name id="278">translatedTimestamp</name>
<stringValue id="279">TranslationTimestamp</stringValue>
<type>VARIABLE</type>
</item>
<item id="280" type="com.j2fe.workflow.definition.Parameter">
<input>true</input>
<name id="281">wantedOutputType</name>
<objectValue id="282" type="com.j2fe.translation.InputType">Text</objectValue>
<type>CONSTANT</type>
</item>
</parameters>
<sources id="283" type="java.util.HashSet">
<item id="284" type="com.j2fe.workflow.definition.Transition">
<name id="285">loop</name>
<source id="286">
<activation>INVOKE</activation>
<clusteredCall>false</clusteredCall>
<directJoin>false</directJoin>
<name id="287">For Loop</name>
<nodeHandler>com.j2fe.workflow.handler.impl.ForEach</nodeHandler>
<nodeHandlerClass id="288">com.j2fe.workflow.handler.impl.ForEach</nodeHandlerClass>
<parameters id="289" type="java.util.HashSet">
<item id="290" type="com.j2fe.workflow.definition.Parameter">
<input>true</input>
<name id="291">counter</name>
<stringValue id="292">counter</stringValue>
<type>VARIABLE</type>
</item>
<item id="293" type="com.j2fe.workflow.definition.Parameter">
<input>false</input>
<name id="294">counter</name>
<stringValue id="295">counter</stringValue>
<type>VARIABLE</type>
</item>
<item id="296" type="com.j2fe.workflow.definition.Parameter">
<input>true</input>
<name id="297">input</name>
<stringValue id="298">messageArray</stringValue>
<type>VARIABLE</type>
</item>
<item id="299" type="com.j2fe.workflow.definition.Parameter">
<input>false</input>
<name id="300">output</name>
<stringValue id="301">Output</stringValue>
<type>VARIABLE</type>
</item>
<item id="302" type="com.j2fe.workflow.definition.Parameter">
<input>false</input>
<name id="303">outputObjects</name>
<stringValue id="304">IncrementedObjects</stringValue>
<type>VARIABLE</type>
</item>
</parameters>
<sources id="305" type="java.util.HashSet">
<item id="306" type="com.j2fe.workflow.definition.Transition">
<name id="307">ToSplit</name>
<source idref="212"/>
<target idref="286"/>
</item>
</sources>
<targets id="308" type="java.util.HashSet">
<item id="309" type="com.j2fe.workflow.definition.Transition">
<name id="310">end-loop</name>
<source idref="286"/>
<target id="311">
<activation>INVOKE</activation>
<clusteredCall>false</clusteredCall>
<directJoin>false</directJoin>
<name id="312">Switch Case</name>
<nodeHandler>com.j2fe.workflow.handler.impl.SwitchCaseSplit</nodeHandler>
<nodeHandlerClass id="313">com.j2fe.workflow.handler.impl.SwitchCaseSplit</nodeHandlerClass>
<parameters id="314" type="java.util.HashSet">
<item id="315" type="com.j2fe.workflow.definition.Parameter">
<input>true</input>
<name id="316">caseItem</name>
<stringValue id="317">IsWorkstationMessage</stringValue>
<type>VARIABLE</type>
</item>
<item id="318" type="com.j2fe.workflow.definition.Parameter">
<input>true</input>
<name id="319">nullTransition</name>
<stringValue id="320">false</stringValue>
<type>CONSTANT</type>
</item>
</parameters>
<sources id="321" type="java.util.HashSet">
<item idref="309" type="com.j2fe.workflow.definition.Transition"/>
</sources>
<targets id="322" type="java.util.HashSet">
<item id="323" type="com.j2fe.workflow.definition.Transition">
<name id="324">false</name>
<source idref="311"/>
<target id="325">
<activation>INVOKE</activation>
<clusteredCall>false</clusteredCall>
<directJoin>false</directJoin>
<name id="326">Process  Transaction</name>
<nodeHandler>com.j2fe.tp.activities.ProcessTransaction</nodeHandler>
<nodeHandlerClass id="327">com.j2fe.tp.activities.ProcessTransaction</nodeHandlerClass>
<parameters id="328" type="java.util.HashSet">
<item id="329" type="com.j2fe.workflow.definition.Parameter">
<input>true</input>
<name id="330">engineResource</name>
<stringValue id="331">engine/TPS-1</stringValue>
<type>REFERENCE</type>
</item>
<item id="332" type="com.j2fe.workflow.definition.Parameter">
<input>true</input>
<name id="333">messageMetaData</name>
<stringValue id="334">MessageMetaData</stringValue>
<type>VARIABLE</type>
</item>
<item id="335" type="com.j2fe.workflow.definition.Parameter">
<input>true</input>
<name id="336">messageType</name>
<stringValue id="337">MessageType</stringValue>
<type>VARIABLE</type>
</item>
<item id="338" type="com.j2fe.workflow.definition.Parameter">
<input>false</input>
<name id="339">processed</name>
<stringValue id="340">Processed</stringValue>
<type>VARIABLE</type>
</item>
<item id="341" type="com.j2fe.workflow.definition.Parameter">
<input>false</input>
<name id="342">processedEntityInfo</name>
<stringValue id="343">ProcessedEntityInformations</stringValue>
<type>VARIABLE</type>
</item>
<item id="344" type="com.j2fe.workflow.definition.Parameter">
<input>true</input>
<name id="345">textInput</name>
<stringValue id="346">TranslatedMessages</stringValue>
<type>VARIABLE</type>
</item>
<item id="347" type="com.j2fe.workflow.definition.Parameter">
<input>true</input>
<name id="348">transactionId</name>
<stringValue id="349">TransactionId</stringValue>
<type>VARIABLE</type>
</item>
</parameters>
<sources id="350" type="java.util.HashSet">
<item idref="323" type="com.j2fe.workflow.definition.Transition"/>
</sources>
<targets id="351" type="java.util.HashSet">
<item id="352" type="com.j2fe.workflow.definition.Transition">
<name id="353">goto-next</name>
<source idref="325"/>
<target id="354">
<activation>INVOKE</activation>
<clusteredCall>false</clusteredCall>
<directJoin>false</directJoin>
<name id="355">Close Transaction</name>
<nodeHandler>com.j2fe.streetlamp.activities.CloseTransaction</nodeHandler>
<nodeHandlerClass id="356">com.j2fe.streetlamp.activities.CloseTransaction</nodeHandlerClass>
<parameters id="357" type="java.util.HashSet">
<item id="358" type="com.j2fe.workflow.definition.Parameter">
<input>false</input>
<name id="359">maxSeverity</name>
<stringValue id="360">Severity</stringValue>
<type>VARIABLE</type>
</item>
<item id="361" type="com.j2fe.workflow.definition.Parameter">
<input>true</input>
<name id="362">messageType</name>
<stringValue id="363">MessageType</stringValue>
<type>VARIABLE</type>
</item>
<item id="364" type="com.j2fe.workflow.definition.Parameter">
<input>true</input>
<name id="365">transactionId</name>
<stringValue id="366">TransactionId</stringValue>
<type>VARIABLE</type>
</item>
<item id="367" type="com.j2fe.workflow.definition.Parameter">
<input>true</input>
<name id="368">transactionMetaData</name>
<stringValue id="369">firstMessageMetaData</stringValue>
<type>VARIABLE</type>
</item>
</parameters>
<sources id="370" type="java.util.HashSet">
<item idref="352" type="com.j2fe.workflow.definition.Transition"/>
<item id="371" type="com.j2fe.workflow.definition.Transition">
<name id="372">goto-next</name>
<source id="373">
<activation>INVOKE</activation>
<clusteredCall>false</clusteredCall>
<directJoin>false</directJoin>
<name id="374">Process Transaction UI</name>
<nodeHandler>com.j2fe.tp.activities.ProcessTransaction</nodeHandler>
<nodeHandlerClass id="375">com.j2fe.tp.activities.ProcessTransaction</nodeHandlerClass>
<parameters id="376" type="java.util.HashSet">
<item id="377" type="com.j2fe.workflow.definition.Parameter">
<input>true</input>
<name id="378">engineResource</name>
<stringValue id="379">engine/TPS-UI</stringValue>
<type>REFERENCE</type>
</item>
<item id="380" type="com.j2fe.workflow.definition.Parameter">
<input>true</input>
<name id="381">messageMetaData</name>
<stringValue id="382">MessageMetaData</stringValue>
<type>VARIABLE</type>
</item>
<item id="383" type="com.j2fe.workflow.definition.Parameter">
<input>true</input>
<name id="384">messageType</name>
<stringValue id="385">MessageType</stringValue>
<type>VARIABLE</type>
</item>
<item id="386" type="com.j2fe.workflow.definition.Parameter">
<input>false</input>
<name id="387">processed</name>
<stringValue id="388">Processed</stringValue>
<type>VARIABLE</type>
</item>
<item id="389" type="com.j2fe.workflow.definition.Parameter">
<input>false</input>
<name id="390">processedEntityInfo</name>
<stringValue id="391">ProcessedEntityInformations</stringValue>
<type>VARIABLE</type>
</item>
<item id="392" type="com.j2fe.workflow.definition.Parameter">
<input>true</input>
<name id="393">textInput</name>
<stringValue id="394">TranslatedMessages</stringValue>
<type>VARIABLE</type>
</item>
<item id="395" type="com.j2fe.workflow.definition.Parameter">
<input>true</input>
<name id="396">transactionId</name>
<stringValue id="397">TransactionId</stringValue>
<type>VARIABLE</type>
</item>
</parameters>
<sources id="398" type="java.util.HashSet">
<item id="399" type="com.j2fe.workflow.definition.Transition">
<name id="400">true</name>
<source idref="311"/>
<target idref="373"/>
</item>
</sources>
<targets id="401" type="java.util.HashSet">
<item idref="371" type="com.j2fe.workflow.definition.Transition"/>
</targets>
<type>ACTIVITY</type>
</source>
<target idref="354"/>
</item>
</sources>
<targets id="402" type="java.util.HashSet">
<item id="403" type="com.j2fe.workflow.definition.Transition">
<name id="404">goto-next</name>
<source idref="354"/>
<target id="405">
<activation>INVOKE</activation>
<clusteredCall>false</clusteredCall>
<directJoin>false</directJoin>
<name id="406">Is Null</name>
<nodeHandler>com.j2fe.general.activities.IsNull</nodeHandler>
<nodeHandlerClass id="407">com.j2fe.general.activities.IsNull</nodeHandlerClass>
<parameters id="408" type="java.util.HashSet">
<item id="409" type="com.j2fe.workflow.definition.Parameter">
<input>true</input>
<name id="410">input</name>
<stringValue id="411">ProcessedEntityInformations</stringValue>
<type>VARIABLE</type>
</item>
</parameters>
<sources id="412" type="java.util.HashSet">
<item idref="403" type="com.j2fe.workflow.definition.Transition"/>
</sources>
<targets id="413" type="java.util.HashSet">
<item id="414" type="com.j2fe.workflow.definition.Transition">
<name id="415">false</name>
<source idref="405"/>
<target id="416">
<activation>INVOKE</activation>
<clusteredCall>false</clusteredCall>
<directJoin>false</directJoin>
<name id="417">Array Helper</name>
<nodeHandler>com.j2fe.general.activities.ArrayHelper</nodeHandler>
<nodeHandlerClass id="418">com.j2fe.general.activities.ArrayHelper</nodeHandlerClass>
<parameters id="419" type="java.util.HashSet">
<item id="420" type="com.j2fe.workflow.definition.Parameter">
<input>true</input>
<name id="421">array</name>
<stringValue id="422">ProcessedEntityInformations</stringValue>
<type>VARIABLE</type>
</item>
</parameters>
<sources id="423" type="java.util.HashSet">
<item idref="414" type="com.j2fe.workflow.definition.Transition"/>
</sources>
<targets id="424" type="java.util.HashSet">
<item id="425" type="com.j2fe.workflow.definition.Transition">
<name id="426">empty</name>
<source idref="416"/>
<target id="427">
<activation>INVOKE</activation>
<clusteredCall>false</clusteredCall>
<directJoin>false</directJoin>
<name id="428">NOP</name>
<nodeHandler>com.j2fe.workflow.handler.impl.DummyActivityHandler</nodeHandler>
<nodeHandlerClass id="429">com.j2fe.workflow.handler.impl.DummyActivityHandler</nodeHandlerClass>
<sources id="430" type="java.util.HashSet">
<item idref="425" type="com.j2fe.workflow.definition.Transition"/>
<item id="431" type="com.j2fe.workflow.definition.Transition">
<name id="432">goto-next</name>
<source id="433">
<activation>INVOKE</activation>
<clusteredCall>false</clusteredCall>
<directJoin>false</directJoin>
<name id="434">Trigger Message Publishing Events</name>
<nodeHandler>com.j2fe.tp.activities.TriggerPublishing</nodeHandler>
<nodeHandlerClass id="435">com.j2fe.tp.activities.TriggerPublishing</nodeHandlerClass>
<parameters id="436" type="java.util.HashSet">
<item id="437" type="com.j2fe.workflow.definition.Parameter">
<input>true</input>
<name id="438">internalPublishingEvent</name>
<stringValue id="439">InternalPublishingEvent</stringValue>
<type>VARIABLE</type>
</item>
<item id="440" type="com.j2fe.workflow.definition.Parameter">
<input>true</input>
<name id="441">messageType</name>
<stringValue id="442">MessageType</stringValue>
<type>VARIABLE</type>
</item>
<item id="443" type="com.j2fe.workflow.definition.Parameter">
<input>true</input>
<name id="444">processedEntityInfo</name>
<stringValue id="445">ProcessedEntityInformations</stringValue>
<type>VARIABLE</type>
</item>
<item id="446" type="com.j2fe.workflow.definition.Parameter">
<input>true</input>
<name id="447">severity</name>
<stringValue id="448">Severity</stringValue>
<type>VARIABLE</type>
</item>
<item id="449" type="com.j2fe.workflow.definition.Parameter">
<input>true</input>
<name id="450">transactionId</name>
<stringValue id="451">TransactionId</stringValue>
<type>VARIABLE</type>
</item>
</parameters>
<sources id="452" type="java.util.HashSet">
<item id="453" type="com.j2fe.workflow.definition.Transition">
<name id="454">false</name>
<source id="455">
<activation>INVOKE</activation>
<clusteredCall>false</clusteredCall>
<directJoin>false</directJoin>
<name id="456">Switch Case</name>
<nodeHandler>com.j2fe.workflow.handler.impl.SwitchCaseSplit</nodeHandler>
<nodeHandlerClass id="457">com.j2fe.workflow.handler.impl.SwitchCaseSplit</nodeHandlerClass>
<parameters id="458" type="java.util.HashSet">
<item id="459" type="com.j2fe.workflow.definition.Parameter">
<input>true</input>
<name id="460">caseItem</name>
<stringValue id="461">CheckForDoNotPostFlag</stringValue>
<type>VARIABLE</type>
</item>
<item id="462" type="com.j2fe.workflow.definition.Parameter">
<input>true</input>
<name id="463">defaultItem</name>
<stringValue id="464">false</stringValue>
<type>CONSTANT</type>
</item>
<item id="465" type="com.j2fe.workflow.definition.Parameter">
<input>true</input>
<name id="466">nullTransition</name>
<stringValue id="467">false</stringValue>
<type>CONSTANT</type>
</item>
</parameters>
<sources id="468" type="java.util.HashSet">
<item id="469" type="com.j2fe.workflow.definition.Transition">
<name id="470">not-empty</name>
<source idref="416"/>
<target idref="455"/>
</item>
</sources>
<targets id="471" type="java.util.HashSet">
<item idref="453" type="com.j2fe.workflow.definition.Transition"/>
<item id="472" type="com.j2fe.workflow.definition.Transition">
<name id="473">true</name>
<source idref="455"/>
<target idref="427"/>
</item>
</targets>
<type>XORSPLIT</type>
</source>
<target idref="433"/>
</item>
</sources>
<targets id="474" type="java.util.HashSet">
<item idref="431" type="com.j2fe.workflow.definition.Transition"/>
</targets>
<type>ACTIVITY</type>
</source>
<target idref="427"/>
</item>
<item id="475" type="com.j2fe.workflow.definition.Transition">
<name id="476">true</name>
<source idref="405"/>
<target idref="427"/>
</item>
<item idref="472" type="com.j2fe.workflow.definition.Transition"/>
</sources>
<targets id="477" type="java.util.HashSet">
<item id="478" type="com.j2fe.workflow.definition.Transition">
<name id="479">goto-next</name>
<source idref="427"/>
<target idref="2"/>
</item>
</targets>
<type>ACTIVITY</type>
</target>
</item>
<item idref="469" type="com.j2fe.workflow.definition.Transition"/>
</targets>
<type>XORSPLIT</type>
</target>
</item>
<item idref="475" type="com.j2fe.workflow.definition.Transition"/>
</targets>
<type>XORSPLIT</type>
</target>
</item>
</targets>
<type>ACTIVITY</type>
</target>
</item>
</targets>
<type>ACTIVITY</type>
</target>
</item>
<item idref="399" type="com.j2fe.workflow.definition.Transition"/>
</targets>
<type>XORSPLIT</type>
</target>
</item>
<item idref="284" type="com.j2fe.workflow.definition.Transition"/>
</targets>
<type>XORSPLIT</type>
</source>
<target idref="249"/>
</item>
</sources>
<targets id="480" type="java.util.HashSet">
<item idref="247" type="com.j2fe.workflow.definition.Transition"/>
</targets>
<type>ACTIVITY</type>
</source>
<target idref="219"/>
</item>
</sources>
<targets id="481" type="java.util.HashSet">
<item idref="217" type="com.j2fe.workflow.definition.Transition"/>
</targets>
<type>ACTIVITY</type>
</source>
<target idref="212"/>
</item>
<item idref="210" type="com.j2fe.workflow.definition.Transition"/>
</sources>
<targets id="482" type="java.util.HashSet">
<item idref="306" type="com.j2fe.workflow.definition.Transition"/>
</targets>
<type>ACTIVITY</type>
</target>
</item>
</targets>
<type>XORSPLIT</type>
</source>
<target idref="151"/>
</item>
</sources>
<targets id="483" type="java.util.HashSet">
<item id="484" type="com.j2fe.workflow.definition.Transition">
<name id="485">empty</name>
<source idref="151"/>
<target idref="34"/>
</item>
<item idref="149" type="com.j2fe.workflow.definition.Transition"/>
</targets>
<type>XORSPLIT</type>
</source>
<target idref="134"/>
</item>
</sources>
<targets id="486" type="java.util.HashSet">
<item idref="132" type="com.j2fe.workflow.definition.Transition"/>
<item id="487" type="com.j2fe.workflow.definition.Transition">
<name id="488">true</name>
<source idref="134"/>
<target id="489">
<activation>INVOKE</activation>
<clusteredCall>false</clusteredCall>
<directJoin>false</directJoin>
<name id="490">Override Filter?</name>
<nodeHandler>com.j2fe.workflow.handler.impl.SwitchCaseSplit</nodeHandler>
<nodeHandlerClass id="491">com.j2fe.workflow.handler.impl.SwitchCaseSplit</nodeHandlerClass>
<parameters id="492" type="java.util.HashSet">
<item id="493" type="com.j2fe.workflow.definition.Parameter">
<input>true</input>
<name id="494">caseItem</name>
<stringValue id="495">ProcessFilteredMessages</stringValue>
<type>VARIABLE</type>
</item>
<item id="496" type="com.j2fe.workflow.definition.Parameter">
<input>true</input>
<name id="497">defaultItem</name>
<stringValue id="498">false</stringValue>
<type>CONSTANT</type>
</item>
<item id="499" type="com.j2fe.workflow.definition.Parameter">
<input>true</input>
<name id="500">nullTransition</name>
<stringValue id="501">false</stringValue>
<type>CONSTANT</type>
</item>
</parameters>
<sources id="502" type="java.util.HashSet">
<item idref="487" type="com.j2fe.workflow.definition.Transition"/>
</sources>
<targets id="503" type="java.util.HashSet">
<item id="504" type="com.j2fe.workflow.definition.Transition">
<name id="505">false</name>
<source idref="489"/>
<target id="506">
<activation>INVOKE</activation>
<clusteredCall>false</clusteredCall>
<directJoin>false</directJoin>
<name id="507">Close Filtered Transaction</name>
<nodeHandler>com.j2fe.streetlamp.activities.CloseTransaction</nodeHandler>
<nodeHandlerClass id="508">com.j2fe.streetlamp.activities.CloseTransaction</nodeHandlerClass>
<parameters id="509" type="java.util.HashSet">
<item id="510" type="com.j2fe.workflow.definition.Parameter">
<input>true</input>
<name id="511">filtered</name>
<objectValue id="512" type="java.lang.Boolean">true</objectValue>
<type>CONSTANT</type>
</item>
<item id="513" type="com.j2fe.workflow.definition.Parameter">
<input>true</input>
<name id="514">formattedMessages</name>
<stringValue id="515">Translated</stringValue>
<type>VARIABLE</type>
</item>
<item id="516" type="com.j2fe.workflow.definition.Parameter">
<input>false</input>
<name id="517">maxSeverity</name>
<stringValue id="518">Severity</stringValue>
<type>VARIABLE</type>
</item>
<item id="519" type="com.j2fe.workflow.definition.Parameter">
<input>true</input>
<name id="520">messageType</name>
<stringValue id="521">MessageType</stringValue>
<type>VARIABLE</type>
</item>
<item id="522" type="com.j2fe.workflow.definition.Parameter">
<input>true</input>
<name id="523">submittedMessage</name>
<stringValue id="524">Message</stringValue>
<type>VARIABLE</type>
</item>
<item id="525" type="com.j2fe.workflow.definition.Parameter">
<input>true</input>
<name id="526">transactionId</name>
<stringValue id="527">TransactionId</stringValue>
<type>VARIABLE</type>
</item>
<item id="528" type="com.j2fe.workflow.definition.Parameter">
<input>true</input>
<name id="529">transactionMetaData</name>
<stringValue id="530">PublishingTranslatedOutput</stringValue>
<type>VARIABLE</type>
<variablePart id="531">metaData</variablePart>
</item>
<item id="532" type="com.j2fe.workflow.definition.Parameter">
<input>true</input>
<name id="533">translatedTimestamp</name>
<stringValue id="534">TranslatedTimestamp</stringValue>
<type>VARIABLE</type>
</item>
</parameters>
<sources id="535" type="java.util.HashSet">
<item idref="504" type="com.j2fe.workflow.definition.Transition"/>
</sources>
<targets id="536" type="java.util.HashSet">
<item id="537" type="com.j2fe.workflow.definition.Transition">
<name id="538">goto-next</name>
<source idref="506"/>
<target id="539">
<activation>INVOKE</activation>
<clusteredCall>false</clusteredCall>
<directJoin>false</directJoin>
<name id="540">Trigger Publishing Events #2</name>
<nodeHandler>com.j2fe.tp.activities.TriggerPublishing</nodeHandler>
<nodeHandlerClass id="541">com.j2fe.tp.activities.TriggerPublishing</nodeHandlerClass>
<parameters id="542" type="java.util.HashSet">
<item id="543" type="com.j2fe.workflow.definition.Parameter">
<input>true</input>
<name id="544">internalPublishingEvent</name>
<stringValue id="545">InternalPublishingEvent</stringValue>
<type>VARIABLE</type>
</item>
<item id="546" type="com.j2fe.workflow.definition.Parameter">
<input>true</input>
<name id="547">messageType</name>
<stringValue id="548">MessageType</stringValue>
<type>VARIABLE</type>
</item>
<item id="549" type="com.j2fe.workflow.definition.Parameter">
<input>true</input>
<name id="550">processedEntityInfo</name>
<stringValue id="551">ProcessedEntityInformations</stringValue>
<type>VARIABLE</type>
</item>
<item id="552" type="com.j2fe.workflow.definition.Parameter">
<input>true</input>
<name id="553">severity</name>
<stringValue id="554">Severity</stringValue>
<type>VARIABLE</type>
</item>
<item id="555" type="com.j2fe.workflow.definition.Parameter">
<input>true</input>
<name id="556">transactionId</name>
<stringValue id="557">TransactionId</stringValue>
<type>VARIABLE</type>
</item>
</parameters>
<sources id="558" type="java.util.HashSet">
<item id="559" type="com.j2fe.workflow.definition.Transition">
<name id="560">false</name>
<source id="561">
<activation>INVOKE</activation>
<clusteredCall>false</clusteredCall>
<directJoin>false</directJoin>
<name id="562">Switch Case</name>
<nodeHandler>com.j2fe.workflow.handler.impl.SwitchCaseSplit</nodeHandler>
<nodeHandlerClass id="563">com.j2fe.workflow.handler.impl.SwitchCaseSplit</nodeHandlerClass>
<parameters id="564" type="java.util.HashSet">
<item id="565" type="com.j2fe.workflow.definition.Parameter">
<input>true</input>
<name id="566">caseItem</name>
<stringValue id="567">CheckForDoNotPostFlag</stringValue>
<type>VARIABLE</type>
</item>
<item id="568" type="com.j2fe.workflow.definition.Parameter">
<input>true</input>
<name id="569">defaultItem</name>
<stringValue id="570">false</stringValue>
<type>CONSTANT</type>
</item>
<item id="571" type="com.j2fe.workflow.definition.Parameter">
<input>true</input>
<name id="572">nullTransition</name>
<stringValue id="573">false</stringValue>
<type>CONSTANT</type>
</item>
</parameters>
<sources id="574" type="java.util.HashSet">
<item id="575" type="com.j2fe.workflow.definition.Transition">
<name id="576">not-empty</name>
<source idref="14"/>
<target idref="561"/>
</item>
</sources>
<targets id="577" type="java.util.HashSet">
<item idref="559" type="com.j2fe.workflow.definition.Transition"/>
<item id="578" type="com.j2fe.workflow.definition.Transition">
<name id="579">true</name>
<source idref="561"/>
<target idref="8"/>
</item>
</targets>
<type>XORSPLIT</type>
</source>
<target idref="539"/>
</item>
<item idref="537" type="com.j2fe.workflow.definition.Transition"/>
</sources>
<targets id="580" type="java.util.HashSet">
<item id="581" type="com.j2fe.workflow.definition.Transition">
<name id="582">goto-next</name>
<source idref="539"/>
<target idref="8"/>
</item>
</targets>
<type>ACTIVITY</type>
</target>
</item>
</targets>
<type>ACTIVITY</type>
</target>
</item>
<item id="583" type="com.j2fe.workflow.definition.Transition">
<name id="584">true</name>
<source idref="489"/>
<target idref="127"/>
</item>
</targets>
<type>XORSPLIT</type>
</target>
</item>
</targets>
<type>XORSPLIT</type>
</source>
<target idref="127"/>
</item>
<item idref="583" type="com.j2fe.workflow.definition.Transition"/>
</sources>
<targets id="585" type="java.util.HashSet">
<item idref="125" type="com.j2fe.workflow.definition.Transition"/>
</targets>
<type>ACTIVITY</type>
</source>
<target idref="114"/>
</item>
</sources>
<targets id="586" type="java.util.HashSet">
<item idref="112" type="com.j2fe.workflow.definition.Transition"/>
<item id="587" type="com.j2fe.workflow.definition.Transition">
<name id="588">true</name>
<source idref="114"/>
<target id="589">
<activation>INVOKE</activation>
<clusteredCall>false</clusteredCall>
<directJoin>false</directJoin>
<name id="590">Process UI Message</name>
<nodeHandler>com.j2fe.tp.activities.ProcessTransaction</nodeHandler>
<nodeHandlerClass id="591">com.j2fe.tp.activities.ProcessTransaction</nodeHandlerClass>
<parameters id="592" type="java.util.HashSet">
<item id="593" type="com.j2fe.workflow.definition.Parameter">
<input>true</input>
<name id="594">engineResource</name>
<stringValue id="595">engine/TPS-UI</stringValue>
<type>REFERENCE</type>
</item>
<item id="596" type="com.j2fe.workflow.definition.Parameter">
<input>true</input>
<name id="597">inputFromTranslation</name>
<stringValue id="598">PublishingTranslatedOutput</stringValue>
<type>VARIABLE</type>
</item>
<item id="599" type="com.j2fe.workflow.definition.Parameter">
<input>true</input>
<name id="600">messageMetaData</name>
<stringValue id="601">MessageMetaData</stringValue>
<type>VARIABLE</type>
</item>
<item id="602" type="com.j2fe.workflow.definition.Parameter">
<input>true</input>
<name id="603">messageType</name>
<stringValue id="604">MessageType</stringValue>
<type>VARIABLE</type>
</item>
<item id="605" type="com.j2fe.workflow.definition.Parameter">
<input>false</input>
<name id="606">processed</name>
<stringValue id="607">Processed</stringValue>
<type>VARIABLE</type>
</item>
<item id="608" type="com.j2fe.workflow.definition.Parameter">
<input>false</input>
<name id="609">processedEntityInfo</name>
<stringValue id="610">ProcessedEntityInformations</stringValue>
<type>VARIABLE</type>
</item>
<item id="611" type="com.j2fe.workflow.definition.Parameter">
<input>false</input>
<name id="612">severity.severity</name>
<stringValue id="613">Severity</stringValue>
<type>VARIABLE</type>
</item>
<item id="614" type="com.j2fe.workflow.definition.Parameter">
<input>true</input>
<name id="615">transactionId</name>
<stringValue id="616">TransactionId</stringValue>
<type>VARIABLE</type>
</item>
</parameters>
<sources id="617" type="java.util.HashSet">
<item idref="587" type="com.j2fe.workflow.definition.Transition"/>
</sources>
<targets id="618" type="java.util.HashSet">
<item id="619" type="com.j2fe.workflow.definition.Transition">
<name id="620">goto-next</name>
<source idref="589"/>
<target idref="76"/>
</item>
</targets>
<type>ACTIVITY</type>
</target>
</item>
</targets>
<type>XORSPLIT</type>
</source>
<target idref="83"/>
</item>
</sources>
<targets id="621" type="java.util.HashSet">
<item idref="81" type="com.j2fe.workflow.definition.Transition"/>
</targets>
<type>ACTIVITY</type>
</source>
<target idref="76"/>
</item>
<item idref="619" type="com.j2fe.workflow.definition.Transition"/>
</sources>
<targets id="622" type="java.util.HashSet">
<item idref="74" type="com.j2fe.workflow.definition.Transition"/>
</targets>
<type>ACTIVITY</type>
</source>
<target idref="66"/>
</item>
</sources>
<targets id="623" type="java.util.HashSet">
<item idref="64" type="com.j2fe.workflow.definition.Transition"/>
<item id="624" type="com.j2fe.workflow.definition.Transition">
<name id="625">goto-next</name>
<source idref="66"/>
<target id="626">
<activation>INVOKE</activation>
<clusteredCall>false</clusteredCall>
<directJoin>false</directJoin>
<name id="627">Call Store Vendor Data</name>
<nodeHandler>com.j2fe.workflow.handler.impl.CallSubWorkflow</nodeHandler>
<nodeHandlerClass id="628">com.j2fe.workflow.handler.impl.CallSubWorkflow</nodeHandlerClass>
<parameters id="629" type="java.util.HashSet">
<item id="630" type="com.j2fe.workflow.definition.Parameter">
<UITypeHint id="631">["Message"]@java/lang/String@</UITypeHint>
<input>true</input>
<name id="632">input["Message"]</name>
<stringValue id="633">Message</stringValue>
<type>VARIABLE</type>
</item>
<item id="634" type="com.j2fe.workflow.definition.Parameter">
<UITypeHint id="635">["MessageType"]@java/lang/String@</UITypeHint>
<input>true</input>
<name id="636">input["MessageType"]</name>
<stringValue id="637">MessageType</stringValue>
<type>VARIABLE</type>
</item>
<item id="638" type="com.j2fe.workflow.definition.Parameter">
<UITypeHint id="639">["ProcessFilteredMessages"]@java/lang/Boolean@</UITypeHint>
<input>true</input>
<name id="640">input["ProcessFilteredMessages"]</name>
<stringValue id="641">ProcessFilteredMessages</stringValue>
<type>VARIABLE</type>
</item>
<item id="642" type="com.j2fe.workflow.definition.Parameter">
<UITypeHint id="643">["PublishingTranslatedOutput"]@com/j2fe/translation/Result@</UITypeHint>
<input>true</input>
<name id="644">input["PublishingTranslatedOutput"]</name>
<stringValue id="645">PublishingTranslatedOutput</stringValue>
<type>VARIABLE</type>
</item>
<item id="646" type="com.j2fe.workflow.definition.Parameter">
<UITypeHint id="647">["SaveVendorDataType"]@java/lang/String@</UITypeHint>
<input>true</input>
<name id="648">input["SaveVendorDataType"]</name>
<stringValue id="649">MessageMetaData</stringValue>
<type>VARIABLE</type>
<variablePart id="650">["SaveVendorDataType"]</variablePart>
</item>
<item id="651" type="com.j2fe.workflow.definition.Parameter">
<UITypeHint id="652">["TransactionId"]@java/lang/String@</UITypeHint>
<input>true</input>
<name id="653">input["TransactionId"]</name>
<stringValue id="654">TransactionId</stringValue>
<type>VARIABLE</type>
</item>
<item id="655" type="com.j2fe.workflow.definition.Parameter">
<input>true</input>
<name id="656">name</name>
<stringValue id="657">Store Vendor Data</stringValue>
<type>CONSTANT</type>
</item>
</parameters>
<sources id="658" type="java.util.HashSet">
<item idref="624" type="com.j2fe.workflow.definition.Transition"/>
</sources>
<targets id="659" type="java.util.HashSet">
<item id="660" type="com.j2fe.workflow.definition.Transition">
<name id="661">goto-next</name>
<source idref="626"/>
<target idref="34"/>
</item>
</targets>
<type>ACTIVITY</type>
</target>
</item>
</targets>
<type>XORSPLIT</type>
</source>
<target idref="34"/>
</item>
<item idref="484" type="com.j2fe.workflow.definition.Transition"/>
<item idref="660" type="com.j2fe.workflow.definition.Transition"/>
</sources>
<targets id="662" type="java.util.HashSet">
<item idref="32" type="com.j2fe.workflow.definition.Transition"/>
</targets>
<type>ACTIVITY</type>
</source>
<target idref="24"/>
</item>
</sources>
<targets id="663" type="java.util.HashSet">
<item idref="22" type="com.j2fe.workflow.definition.Transition"/>
<item id="664" type="com.j2fe.workflow.definition.Transition">
<name id="665">true</name>
<source idref="24"/>
<target idref="8"/>
</item>
</targets>
<type>XORSPLIT</type>
</source>
<target idref="14"/>
</item>
</sources>
<targets id="666" type="java.util.HashSet">
<item idref="12" type="com.j2fe.workflow.definition.Transition"/>
<item idref="575" type="com.j2fe.workflow.definition.Transition"/>
</targets>
<type>XORSPLIT</type>
</source>
<target idref="8"/>
</item>
<item idref="581" type="com.j2fe.workflow.definition.Transition"/>
<item idref="664" type="com.j2fe.workflow.definition.Transition"/>
<item idref="578" type="com.j2fe.workflow.definition.Transition"/>
</sources>
<targets id="667" type="java.util.HashSet">
<item idref="6" type="com.j2fe.workflow.definition.Transition"/>
</targets>
<type>ACTIVITY</type>
</source>
<target idref="2"/>
</item>
<item idref="478" type="com.j2fe.workflow.definition.Transition"/>
</sources>
<targets id="668" type="java.util.HashSet"/>
<type>END</type>
</endNode>
<forcePurgeAtEnd>false</forcePurgeAtEnd>
<group id="669">Custom/Moca</group>
<haltOnError>false</haltOnError>
<lastChangeUser id="670">KYTL_GC</lastChangeUser>
<lastUpdate id="671">2022-11-05T07:49:03.000+0100</lastUpdate>
<name id="672">Basic Message Processing(copy)</name>
<optimize>true</optimize>
<parameter id="673" type="java.util.HashMap">
<entry>
<key id="674" type="java.lang.String">CheckForDoNotPostFlag</key>
<value id="675" type="com.j2fe.workflow.definition.WorkflowParameter">
<className id="676">java.lang.Boolean</className>
<clazz>java.lang.Boolean</clazz>
<input>true</input>
<output>true</output>
<required>false</required>
</value>
</entry>
<entry>
<key id="677" type="java.lang.String">InternalPublishingEvent</key>
<value id="678" type="com.j2fe.workflow.definition.WorkflowParameter">
<className id="679">java.lang.String</className>
<clazz>java.lang.String</clazz>
<description id="680">Location of the property file containing the connection details of server on which publishing application is deployed.</description>
<input>true</input>
<output>false</output>
<required>false</required>
</value>
</entry>
<entry>
<key id="681" type="java.lang.String">IsWorkstationMessage</key>
<value id="682" type="com.j2fe.workflow.definition.WorkflowParameter">
<className id="683">java.lang.Boolean</className>
<clazz>java.lang.Boolean</clazz>
<input>true</input>
<output>false</output>
<required>false</required>
</value>
</entry>
<entry>
<key id="684" type="java.lang.String">Message</key>
<value id="685" type="com.j2fe.workflow.definition.WorkflowParameter">
<className id="686">java.lang.String</className>
<clazz>java.lang.String</clazz>
<input>true</input>
<output>false</output>
<required>true</required>
</value>
</entry>
<entry>
<key id="687" type="java.lang.String">MessageMetaData</key>
<value id="688" type="com.j2fe.workflow.definition.WorkflowParameter">
<className id="689">java.util.Map</className>
<clazz>java.util.Map</clazz>
<input>true</input>
<output>false</output>
<required>false</required>
</value>
</entry>
<entry>
<key id="690" type="java.lang.String">MessageType</key>
<value id="691" type="com.j2fe.workflow.definition.WorkflowParameter">
<className id="692">java.lang.String</className>
<clazz>java.lang.String</clazz>
<input>true</input>
<output>false</output>
<required>true</required>
</value>
</entry>
<entry>
<key id="693" type="java.lang.String">ProcessFilteredMessages</key>
<value id="694" type="com.j2fe.workflow.definition.WorkflowParameter">
<className id="695">java.lang.Boolean</className>
<clazz>java.lang.Boolean</clazz>
<description id="696">Indicates that messages are processed regardless of the result of the Generic Inbound Filter.</description>
<input>true</input>
<output>false</output>
<required>false</required>
</value>
</entry>
<entry>
<key id="697" type="java.lang.String">Severity</key>
<value id="698" type="com.j2fe.workflow.definition.WorkflowParameter">
<className id="699">java.lang.Integer</className>
<clazz>java.lang.Integer</clazz>
<input>false</input>
<output>true</output>
<required>false</required>
</value>
</entry>
<entry>
<key id="700" type="java.lang.String">TransactionId</key>
<value id="701" type="com.j2fe.workflow.definition.WorkflowParameter">
<className id="702">java.lang.String</className>
<clazz>java.lang.String</clazz>
<input>true</input>
<output>false</output>
<required>true</required>
</value>
</entry>
<entry>
<key id="703" type="java.lang.String">messageArray</key>
<value id="704" type="com.j2fe.workflow.definition.WorkflowParameter">
<className id="705">[Ljava.lang.String;</className>
<clazz>[Ljava.lang.String;</clazz>
<input>true</input>
<output>false</output>
<required>false</required>
</value>
</entry>
</parameter>
<permissions id="706" type="java.util.HashSet"/>
<priority>50</priority>
<purgeAtEnd>true</purgeAtEnd>
<retries>1</retries>
<startNode idref="204"/>
<status>RELEASED</status>
<variables id="707" type="java.util.HashMap">
<entry>
<key id="708" type="java.lang.String">CheckForDoNotPostFlag</key>
<value id="709" type="com.j2fe.workflow.definition.GlobalVariable">
<className id="710">java.lang.Boolean</className>
<clazz>java.lang.Boolean</clazz>
<persistent>false</persistent>
<value id="711" type="java.lang.Boolean">false</value>
</value>
</entry>
<entry>
<key id="712" type="java.lang.String">InternalPublishingEvent</key>
<value id="713" type="com.j2fe.workflow.definition.GlobalVariable">
<className id="714">java.lang.String</className>
<clazz>java.lang.String</clazz>
<description id="715">Location of the property file containing the connection details of server on which publishing application is deployed.</description>
<persistent>false</persistent>
<value id="716" type="java.lang.String">InternalMessagePublishingEvent</value>
</value>
</entry>
<entry>
<key id="717" type="java.lang.String">IsWorkstationMessage</key>
<value id="718" type="com.j2fe.workflow.definition.GlobalVariable">
<className id="719">java.lang.Boolean</className>
<clazz>java.lang.Boolean</clazz>
<persistent>false</persistent>
<value idref="711"/>
</value>
</entry>
<entry>
<key id="720" type="java.lang.String">Message</key>
<value id="721" type="com.j2fe.workflow.definition.GlobalVariable">
<className id="722">java.lang.String</className>
<clazz>java.lang.String</clazz>
<persistent>false</persistent>
</value>
</entry>
<entry>
<key id="723" type="java.lang.String">MessageMetaData</key>
<value id="724" type="com.j2fe.workflow.definition.GlobalVariable">
<className id="725">java.util.Map</className>
<clazz>java.util.Map</clazz>
<persistent>false</persistent>
</value>
</entry>
<entry>
<key id="726" type="java.lang.String">MessageType</key>
<value id="727" type="com.j2fe.workflow.definition.GlobalVariable">
<className id="728">java.lang.String</className>
<clazz>java.lang.String</clazz>
<persistent>false</persistent>
</value>
</entry>
<entry>
<key id="729" type="java.lang.String">ProcessFilteredMessages</key>
<value id="730" type="com.j2fe.workflow.definition.GlobalVariable">
<className id="731">java.lang.Boolean</className>
<clazz>java.lang.Boolean</clazz>
<description id="732">Indicates that messages are processed regardless of the result of the Generic Inbound Filter.</description>
<persistent>false</persistent>
</value>
</entry>
<entry>
<key id="733" type="java.lang.String">Processed</key>
<value id="734" type="com.j2fe.workflow.definition.GlobalVariable">
<className id="735">[[B</className>
<clazz>[[B</clazz>
<description id="736">The Processed Messages</description>
<persistent>false</persistent>
</value>
</entry>
<entry>
<key id="737" type="java.lang.String">Severity</key>
<value id="738" type="com.j2fe.workflow.definition.GlobalVariable">
<className id="739">java.lang.Integer</className>
<clazz>java.lang.Integer</clazz>
<persistent>false</persistent>
<value id="740" type="java.lang.Integer">0</value>
</value>
</entry>
<entry>
<key id="741" type="java.lang.String">TransactionId</key>
<value id="742" type="com.j2fe.workflow.definition.GlobalVariable">
<className id="743">java.lang.String</className>
<clazz>java.lang.String</clazz>
<persistent>false</persistent>
</value>
</entry>
<entry>
<key id="744" type="java.lang.String">TranslatedMessages</key>
<value id="745" type="com.j2fe.workflow.definition.GlobalVariable">
<className id="746">[Ljava.lang.Object;</className>
<clazz>[Ljava.lang.Object;</clazz>
<persistent>false</persistent>
</value>
</entry>
<entry>
<key id="747" type="java.lang.String">counter</key>
<value id="748" type="com.j2fe.workflow.definition.GlobalVariable">
<className id="749">java.lang.Integer</className>
<clazz>java.lang.Integer</clazz>
<persistent>true</persistent>
<value idref="740"/>
</value>
</entry>
<entry>
<key id="750" type="java.lang.String">firstMessageMetaData</key>
<value id="751" type="com.j2fe.workflow.definition.GlobalVariable">
<className id="752">java.lang.Object</className>
<clazz>java.lang.Object</clazz>
<persistent>false</persistent>
</value>
</entry>
<entry>
<key id="753" type="java.lang.String">messageArray</key>
<value id="754" type="com.j2fe.workflow.definition.GlobalVariable">
<className id="755">[Ljava.lang.String;</className>
<clazz>[Ljava.lang.String;</clazz>
<persistent>false</persistent>
</value>
</entry>
</variables>
<version>1</version>
</com.j2fe.workflow.definition.Workflow>
</businessobject>
</goldensource-package>
