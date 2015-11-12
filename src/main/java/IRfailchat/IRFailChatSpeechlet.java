/**
    Copyright 2014-2015 Amazon.com, Inc. or its affiliates. All Rights Reserved.

    Licensed under the Apache License, Version 2.0 (the "License"). You may not use this file except in compliance with the License. A copy of the License is located at

        http://aws.amazon.com/apache2.0/

    or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */
package IRfailchat;

import java.util.ArrayList;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazon.speech.slu.Intent;
import com.amazon.speech.slu.Slot;
import com.amazon.speech.speechlet.IntentRequest;
import com.amazon.speech.speechlet.LaunchRequest;
import com.amazon.speech.speechlet.Session;
import com.amazon.speech.speechlet.SessionEndedRequest;
import com.amazon.speech.speechlet.SessionStartedRequest;
import com.amazon.speech.speechlet.Speechlet;
import com.amazon.speech.speechlet.SpeechletException;
import com.amazon.speech.speechlet.SpeechletResponse;
import com.amazon.speech.ui.SsmlOutputSpeech;
import com.amazon.speech.ui.Reprompt;
import com.amazon.speech.ui.SimpleCard;

/**
 * This sample shows how to create a Lambda function for handling Alexa Skill requests that:
 * 
 * <ul>
 * <li><b>Session State</b>: Handles a multi-turn dialog model.</li>
 * <li><b>LITERAL slot</b>: demonstrates literal handling for a finite set of known values</li>
 * </ul>
 * <p>
 * <h2>Examples</h2>
 * <p>
 * <b>Dialog model</b>
 * <p>
 * User: "Alexa, ask Wise Guy to tell me a knock knock joke."
 * <p>
 * Alexa: "Knock knock"
 * <p>
 * User: "Who's there?"
 * <p>
 * Alexa: "<phrase>"
 * <p>
 * User: "<phrase> who"
 * <p>
 * Alexa: "<Punchline>"
 */
public class IRFailChatSpeechlet implements Speechlet {

    private static final Logger log = LoggerFactory.getLogger(IRFailChatSpeechlet.class);

    /**
     * Session attribute to store the stage the chat is at.
     */
    private static final String SESSION_STAGE = "stage";

    /**
     * Session attribute to store the current AllGoodAnswer ID. TODO: do I need it?
     */
    private static final String SESSION_JOKE_ID = "jokeid";

    /**
     * Session attribute to store the current "call to" id
     */
    private static final String SESSION_CALL_TO_ID = "calltoid";


    /**
     * Stage 1 indicates we've already said are you ok.
     */
    private static final int ARE_YOU_OK_STAGE = 1;
    /**
     * Stage 2 indicates we are in a condition "good" path.
     */
    private static final int CONDITION_GOOD_STAGE = 2;

    /**
     * Stage 2.1 indicates we are in a condition "good" path closure.
     */
    private static final int CONDITION_GOOD_STAGE_CLOSURE = 3;

    /**
     * Stage 3 indicates we are in a condition "good" path.
     */
    private static final int CONDITION_BAD_STAGE = 4;

    /**
     * Stage 3.1 indicates we are in a condition "bad" path closure.
     */
    private static final int CONDITION_BAD_CONFIRM_STAGE = 5;


    /**
     * The Slot Call to name as defined in the json file
     */
    private static final String SLOT_CALL_TO_NAMES = "CallToName";

    /**
     * ArrayList containing "All good" answers.
     */
    private static final ArrayList<AllGoodAnswer> ALL_GOOD_ANSWER_LIST = new ArrayList<AllGoodAnswer>();

    static {// For now speech and card are the same as not clear if card is needed
        ALL_GOOD_ANSWER_LIST.add(new AllGoodAnswer("Glad to hear that","Glad to thear that"));
        ALL_GOOD_ANSWER_LIST.add(new AllGoodAnswer("That's great!", "That's great!"));
        ALL_GOOD_ANSWER_LIST.add(new AllGoodAnswer("Wonderfull! I am so happy", "Wonderfull! I am so happy"));
        ALL_GOOD_ANSWER_LIST.add(new AllGoodAnswer("Great", "Great"));
    }

    /**
     * ArraylList conatining all contacts to call
     */
    private static final ArrayList<String> ALL_CALL_TO_LIST = new ArrayList<String>();

    static {// For now speech and card are the same as not clear if card is needed
        ALL_CALL_TO_LIST.add("Dave");
        ALL_CALL_TO_LIST.add("your Care giver");
        ALL_CALL_TO_LIST.add("Dor");
        ALL_CALL_TO_LIST.add("Your son");
    }
    @Override
    public void onSessionStarted(final SessionStartedRequest request, final Session session)
            throws SpeechletException {
        log.info("onSessionStarted requestId={}, sessionId={}", request.getRequestId(),
                session.getSessionId());

        // any initialization logic goes here
    }

    @Override
    public SpeechletResponse onLaunch(final LaunchRequest request, final Session session)
            throws SpeechletException {
        log.info("onLaunch requestId={}, sessionId={}", request.getRequestId(),
                session.getSessionId());

        return handleAreYouOkIntent(session);
    }

    @Override
    public SpeechletResponse onIntent(final IntentRequest request, final Session session)
            throws SpeechletException {
        log.info("onIntent requestId={}, sessionId={}", request.getRequestId(),
                session.getSessionId());

        Intent intent = request.getIntent();
        String intentName = (intent != null) ? intent.getName() : null;

        if ("CheckConditionIntent".equals(intentName)) {
            return handleAreYouOkIntent(session);
        } else if ("ConditionGoodIntent".equals(intentName)) {
            return handleConditionGood(session);
        } else if ("ConfirmGoodConditionIntent".equals(intentName)) {
            return handleConfirmGoodConditionIntent(session);
        } else if ("ConditionBadIntent".equals(intentName))  {
            return handleConditionBad(session);
        } else if ("ConditionBadConfirmationIntent".equals(intentName))  {
            return handleConditionConfirmationBad(request,session);
        } else if ("HelpIntent".equals(intentName)) { //TODO: update this one
            String speechOutput = "";
            int stage = -1;
            if (session.getAttributes().containsKey(SESSION_STAGE)) {
                stage = (int) session.getAttribute(SESSION_STAGE);
            }
            switch (stage) {
                case 0:
                    speechOutput =
                            "Knock knock jokes are a fun call and response type of joke. "
                                    + "To start the joke, just ask by saying tell me a"
                                    + " joke, or you can say exit.";
                    break;
                case 1:
                    speechOutput = "You can ask, who's there, or you can say exit.";
                    break;
                case 2:
                    speechOutput = "You can ask, who, or you can say exit.";
                    break;
                default:
                    speechOutput =
                            "Knock knock jokes are a fun call and response type of joke. "
                                    + "To start the joke, just ask by saying tell me a "
                                    + "joke, or you can say exit.";
            }

            String repromptText = speechOutput;
            return newAskResponse("<speak>" + speechOutput + "</speak>", "<speak>" + repromptText + "</speak>");
        } else {
            throw new SpeechletException("Invalid Intent");
        }
    }

    @Override
    public void onSessionEnded(final SessionEndedRequest request, final Session session)
            throws SpeechletException {
        log.info("onSessionEnded requestId={}, sessionId={}", request.getRequestId(),
                session.getSessionId());

        // any session cleanup logic would go here
    }

    /**
     * Starts the short chat to simulate falls.
     *
     * @param session
     *            the session object
     * @return SpeechletResponse the speechlet response
     */
    private SpeechletResponse handleAreYouOkIntent(final Session session) {
        String speechOutput = "";

        // Reprompt speech will be triggered if the user doesn't respond.
        String repromptText = "I didn't hear a response, are you ok?";

        // / Select a random joke and store it in the session variables
        //int jokeID = (int) Math.floor(Math.random() * ALL_GOOD_ANSWER_LIST.size());

        // The stage variable tracks the phase of the dialogue.
        // When this function completes, it will be on stage 1.
        session.setAttribute(SESSION_STAGE, ARE_YOU_OK_STAGE);
        //session.setAttribute(SESSION_JOKE_ID, jokeID);
        speechOutput = "Are you ok?";

        // Create the Simple card content.
        SimpleCard card = new SimpleCard();
        card.setTitle("Fail Chat");
        card.setContent(speechOutput);

        SpeechletResponse response = newAskResponse("<speak>" + speechOutput + "</speak>",
                "<speak>" + repromptText + "</speak>");
        response.setCard(card);
        return response;
    }

    /**
     * Responds to the user saying "I'm ok".
     *
     * @param session
     *            the session object
     * @return SpeechletResponse the speechlet response
     */
    private SpeechletResponse handleConditionGood(final Session session) {
        String speechOutput = "", repromptText = "";
        if (session.getAttributes().containsKey(SESSION_STAGE)) {
            if ((int) session.getAttribute(SESSION_STAGE) == ARE_YOU_OK_STAGE) {

                // Retrieve the answer setup text. Randomly now just to explore the multiple answers option
                //  Select a random answer
                int answerID = (int) Math.floor(Math.random() * ALL_GOOD_ANSWER_LIST.size());
                speechOutput = ALL_GOOD_ANSWER_LIST.get(answerID).speechAnswerline;

                // Advance the stage of the dialogue.
                session.setAttribute(SESSION_STAGE, CONDITION_GOOD_STAGE);

                repromptText = "You can ask, " + speechOutput + " who?";

            } else { // TODO: understand when we get here...
                session.setAttribute(SESSION_STAGE, ARE_YOU_OK_STAGE);
                speechOutput = "I was asking if you are ok! <break time=\"0.3s\" /> ok, ok?";
                repromptText = "You can ask for help again";
            }
        } else {
            // If the session attributes are not found, the joke must restart.
            speechOutput =
                    "Sorry, I couldn't hear you well. You can ask for help again.";
            repromptText = "You can ask for help again.";
        }

        return newAskResponse("<speak>" + speechOutput + "</speak>", "<speak>" + repromptText + "</speak>");
    }

    /**
     * Handles users confirmation for all good response
     *
     * @param session
     *            the session object
     * @return SpeechletResponse the speechlet response
     */
    private SpeechletResponse handleConfirmGoodConditionIntent(final Session session) {
        String speechOutput = "", repromptText = "";

        // Create the Simple card content.
        SimpleCard card = new SimpleCard();
        card.setTitle("Fail Chat");

        if (session.getAttributes().containsKey(SESSION_STAGE)) {
            if ((int) session.getAttribute(SESSION_STAGE) == CONDITION_GOOD_STAGE) { // we got here from the all good stage

                speechOutput = "I am always here if you need me";
                card.setContent(speechOutput);

                // Create the plain text output
                SsmlOutputSpeech outputSpeech = new SsmlOutputSpeech();
                outputSpeech.setSsml("<speak>" + speechOutput + "</speak>");

                // If the chat completes successfully, this function will end the active session
                return SpeechletResponse.newTellResponse(outputSpeech, card);
            } else { // something in the flow went wrong... will start from scratch the chat?
                session.setAttribute(SESSION_STAGE, ARE_YOU_OK_STAGE);
                speechOutput = "Not sure you are ok, would like to verify. Are you ok?";
                repromptText = "Not sure you are ok, would like to verify. Are you ok?";

                card.setContent("Not sure you are ok, would like to verify. Are you ok?");

                // Create the plain text output
                SsmlOutputSpeech outputSpeech = new SsmlOutputSpeech();
                outputSpeech.setSsml("<speak>" + speechOutput + "</speak>");
                SsmlOutputSpeech repromptOutputSpeech = new SsmlOutputSpeech();
                repromptOutputSpeech.setSsml("<speak>" + repromptText + "</speak>");
                Reprompt repromptSpeech = new Reprompt();
                repromptSpeech.setOutputSpeech(repromptOutputSpeech);

                // If the chat has to be restarted, then keep the session alive
                return SpeechletResponse.newAskResponse(outputSpeech, repromptSpeech, card);
            }
        } else {
            speechOutput =
                    "Sorry, I couldn't correctly retrieve what you said. You can ask for help if needed";
            repromptText = "You can ask for help if needed";
            card.setContent(speechOutput);
            SpeechletResponse response = newAskResponse("<speak>" + speechOutput + "</speak>",
                    "<speak>" + repromptText + "</speak>");
            response.setCard(card);
            return response;
        }
    }

    /**
     * Responds to the user saying "I'm NOT ok".
     *
     * @param session
     *            the session object
     * @return SpeechletResponse the speechlet response
     */
    private SpeechletResponse handleConditionBad(final Session session) {
        String speechOutput = "", repromptText = "";
        if (session.getAttributes().containsKey(SESSION_STAGE)) {
            if ((int) session.getAttribute(SESSION_STAGE) == ARE_YOU_OK_STAGE) {

                // Retrieve the "call to" text. Name, "son"  Randomly now just to explore the multiple answers option
                //  Selects a random answer
                int callToID = (int) Math.floor(Math.random() * ALL_CALL_TO_LIST.size());
                String callto  = ALL_CALL_TO_LIST.get(callToID);
                session.setAttribute(SESSION_CALL_TO_ID, callToID); // store it in the session.

                speechOutput = "Shall I call" + callto;

                // Advance the stage of the dialogue.
                session.setAttribute(SESSION_STAGE, CONDITION_BAD_STAGE);

                repromptText = "Did you say no?";

            } else { // TODO: understand when we get here...
                session.setAttribute(SESSION_STAGE, ARE_YOU_OK_STAGE);
                speechOutput = "I was asking if you are ok! <break time=\"0.3s\" /> ok, ok?";
                repromptText = "You can ask for help again";
            }
        } else {
            // If the session attributes are not found, the chat must restart.
            speechOutput =
                    "Sorry, I couldn't hear you well. You can ask for help again.";
            repromptText = "You can ask for help again.";
        }

        return newAskResponse("<speak>" + speechOutput + "</speak>", "<speak>" + repromptText + "</speak>");
    }

    /**
     * Responds to the kYou saying "shall I call Dave ("call to").
     *
     * @param session
     *            the session object
     * @return SpeechletResponse the speechlet response
     */
    private SpeechletResponse handleConditionConfirmationBad(final IntentRequest request, final Session session) {
        String speechOutput = "", repromptText = "";
        if (session.getAttributes().containsKey(SESSION_STAGE)) {
            if ((int) session.getAttribute(SESSION_STAGE) == CONDITION_BAD_STAGE) {

                // Retrieve the "call to" text. Name, "son"
                int callToID = (int)session.getAttribute(SESSION_CALL_TO_ID);
                String callto  = ALL_CALL_TO_LIST.get(callToID);
                Map<String, Slot> slots = request.getIntent().getSlots();
                int i;
                for (i=0; i<slots.size();i++)
                {
                    // Get the color Call to  from the list of slots and add them (for debug) to reprompt
                    Slot CurrentCallToSlot = slots.get(SLOT_CALL_TO_NAMES);
                    repromptText = repromptText.concat(CurrentCallToSlot.getName());
                }

                repromptText = repromptText.concat("we had" + i + "slots");

                speechOutput = "Ok, calling" + callto;

                // Advance the stage of the dialogue.
                session.setAttribute(SESSION_STAGE, CONDITION_BAD_CONFIRM_STAGE);

                //repromptText = "Didn't hear back. I am calling youe default care giver"; //TODO: think what to do here...

            } else { // TODO: understand when we get here...
                session.setAttribute(SESSION_STAGE, ARE_YOU_OK_STAGE);
                speechOutput = "I was asking if you are ok! <break time=\"0.3s\" /> ok, ok?";
                repromptText = "You can ask for help again";
            }
        } else {
            // If the session attributes are not found, the chat must restart.
            speechOutput =
                    "Sorry, I couldn't hear you well. You can ask for help again.";
            repromptText = "You can ask for help again.";
        }

        return newAskResponse("<speak>" + speechOutput + "</speak>", "<speak>" + repromptText + "</speak>");
    }


    /**
     * Delivers the punchline of the joke after the user responds to the setup. original example code
     *
     * @param session
     *            the session object
     * @return SpeechletResponse the speechlet response
     */
    private SpeechletResponse handleConfirmGoodConditionIntent1(final Session session) {
        String speechOutput = "", repromptText = "";

        // Create the Simple card content.
        SimpleCard card = new SimpleCard();
        card.setTitle("Wise Guy");

        if (session.getAttributes().containsKey(SESSION_STAGE)) {
            if ((int) session.getAttribute(SESSION_STAGE) == ARE_YOU_OK_STAGE) {
                int jokeID = (int) session.getAttribute(SESSION_JOKE_ID);
                speechOutput = ALL_GOOD_ANSWER_LIST.get(jokeID).speechAnswerline;
                card.setContent(ALL_GOOD_ANSWER_LIST.get(jokeID).cardAnswerline);

                // Create the plain text output
                SsmlOutputSpeech outputSpeech = new SsmlOutputSpeech();
                outputSpeech.setSsml("<speak>" + speechOutput + "</speak>");

                // If the joke completes successfully, this function will end the active session
                return SpeechletResponse.newTellResponse(outputSpeech, card);
            } else {
                session.setAttribute(SESSION_STAGE, CONDITION_GOOD_STAGE);
                speechOutput = "That's not how how knock knock jokes work! <break time=\"0.3s\" /> Knock knock";
                repromptText = "You can ask who's there.";

                card.setContent("That's not how knock knock jokes work! Knock knock");

                // Create the plain text output
                SsmlOutputSpeech outputSpeech = new SsmlOutputSpeech();
                outputSpeech.setSsml("<speak>" + speechOutput + "</speak>");
                SsmlOutputSpeech repromptOutputSpeech = new SsmlOutputSpeech();
                repromptOutputSpeech.setSsml("<speak>" + repromptText + "</speak>");
                Reprompt repromptSpeech = new Reprompt();
                repromptSpeech.setOutputSpeech(repromptOutputSpeech);

                // If the joke has to be restarted, then keep the session alive
                return SpeechletResponse.newAskResponse(outputSpeech, repromptSpeech, card);
            }
        } else {
            speechOutput =
                    "Sorry, I couldn't correctly retrieve the joke. You can say, tell me a joke";
            repromptText = "You can say, tell me a joke";
            card.setContent(speechOutput);
            SpeechletResponse response = newAskResponse("<speak>" + speechOutput + "</speak>",
                    "<speak>" + repromptText + "</speak>");
            response.setCard(card);
            return response;
        }
    }

    /**
     * Wrapper for creating the Ask response from the input strings.
     * 
     * @param stringOutput
     *            the output to be spoken
     * @param repromptText
     *            the reprompt for if the user doesn't reply or is misunderstood.
     * @return SpeechletResponse the speechlet response
     */
    private SpeechletResponse newAskResponse(String stringOutput, String repromptText) {
        SsmlOutputSpeech outputSpeech = new SsmlOutputSpeech();
        outputSpeech.setSsml(stringOutput);
        SsmlOutputSpeech repromptOutputSpeech = new SsmlOutputSpeech();
        repromptOutputSpeech.setSsml(repromptText);
        Reprompt reprompt = new Reprompt();
        reprompt.setOutputSpeech(repromptOutputSpeech);
        return SpeechletResponse.newAskResponse(outputSpeech, reprompt);
    }

    private static class AllGoodAnswer {

        private final String speechAnswerline;
        private final String cardAnswerline;

        AllGoodAnswer(String speechPunchline, String cardPunchline) {
            this.speechAnswerline = speechPunchline;
            this.cardAnswerline = cardPunchline;
        }
    }

}
