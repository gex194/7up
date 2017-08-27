package io.opros;


import org.telegram.telegrambots.api.methods.send.SendMessage;
import org.telegram.telegrambots.api.objects.Update;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.exceptions.TelegramApiException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import static io.opros.OprosBot.State.*;
import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;

public class OprosBot extends TelegramLongPollingBot {
    enum State { NOT_REGISTERED, WAITING_QUIZ, IN_PROGRESS_QUIZ}

    private static Map<String, UserData> users = new HashMap<>();
    private static Set<Poll> polls = new HashSet<Poll>();


    static {
        users.put("northcapen", new UserData(NOT_REGISTERED));
        users.put("melsrose", new UserData(NOT_REGISTERED));
        users.put("gex194", new UserData(NOT_REGISTERED));

        Question question = new Question(1L, "Побуждает ли вас к отъезду обстановка в стране?\nДа/Нет", asList("Да", "Нет"));
        Question question2 = new Question(2L, "Представьте: в выборах участвуют Путин и Навальный. За кого будете голосовать?", asList("Путин", "Навальный"));
        Question question3 = new Question(3L, "Власть в России страшная или смешная?", asList("Страшная", "Смешная"));
        Question question4 = new Question(4L, "Если была бы возможность изменить прошлое, Вы бы попробовали?\nДа/Нет", asList("Да", "Нет"));
        Question question5 = new Question(5L, "Если будет война, вы пойдете защищать виноградники Медведева?\n Да/Нет" , asList("Да", "Нет"));
        Question question6 = new Question(6L, "Хотели бы вы жить в Северной Корее?\nДа/Нет", asList("Да", "Нет"));
        Question question7 = new Question(7L, "Как вы относитесь к людям с нетрадиционной ориентацией?\n Хорошо/Плохо/Нейтрально" , asList("Хорошо","Плохо", "Нейтрально"));
        Question question8 = new Question(8L, "Тюрьмы в России людей исправляют или калечат?", asList("Исправляют", "Калечат"));
        Question question9 = new Question(9L, "Любите ли вы пушистых котиков?:)\nДа/Нет?", asList("Да", "Нет"));
        polls.add(new Poll(1L, "Опрос", asList(question, question2, question3, question4, question5,question6,question7,question8, question9)));

    }

    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            String message_text = update.getMessage().getText();
            String result = null;
            String userName = update.getMessage().getFrom().getUserName();
            final UserData userData = users.get(userName);
            State state = userData.state;

            switch (state) {
                case NOT_REGISTERED:
                    if (message_text.equals("/start_register")) {
                        result = "Здесь ссылка для регистрации (/finish_register)";
                    } else if (message_text.equals("/finish_register")) {
                        users.put(userName, new UserData(WAITING_QUIZ));
                        result = "Вы зарегистрированы. Начните опрос (/start_quiz)";
                    } else {
                        result = "Вы еще не зарегистрированы (/start_register)";

                    }
                    break;

                case WAITING_QUIZ:
                    if (message_text.equals("/start_quiz")) {
                        ArrayList<Poll> polls = new ArrayList<>(OprosBot.polls);
                        Poll poll = polls.get(0);
                        userData.pollId = poll.id;
                        userData.questionId = 1L;
                        userData.state = IN_PROGRESS_QUIZ;

                        result = poll.getQuestion(userData.questionId).text;
                    } else {
                        result = "Нужно начать опрос (/start_quiz)";
                    }
                    break;

                case IN_PROGRESS_QUIZ:
                    Poll poll = polls.stream().filter(p -> Objects.equals(p.id, userData.pollId)).collect(toList()).get(0);
                    Question question = poll.getQuestion(userData.questionId);
                    if(userData.questionId  == poll.questions.size()) {
                        userData.state = State.WAITING_QUIZ;
                        result = "Спасибо за опрос. Ваши деньги очень скоро придут на Ваш счет!";
                    }
                    else if (question.answer.contains(message_text)) {
                        userData.questionId = userData.questionId + 1;
                        result = poll.getQuestion(userData.questionId).text;
                    } else {
                        result = "Вот сейчас не совсем понял. Попробуем еще раз: " + question.text;
                    }


                    break;
            }


            long chat_id = update.getMessage().getChatId();

            SendMessage message = new SendMessage() // Create a message object object
                    .setChatId(chat_id)
                    .setText(result);
            try {
                sendMessage(message); // Sending our message object to user
            } catch (TelegramApiException e) {
                e.printStackTrace();
            }
        }
    }



    public String getBotUsername() {
        return "Opros.io";
    }

    public String getBotToken() {
        return "367476042:AAFwjhhdDEUez704wnHHSNk_rPcY61fKXhk";
    }
}

class UserData {
    OprosBot.State state;
    Long pollId;
    Long questionId;

    public UserData(OprosBot.State state) {
        this.state = state;
    }
}

class Poll {
    Long id;
    String name;
    List<Question> questions = new ArrayList<>();

    public Poll(Long id, String name, List<Question> questions) {
        this.id = id;
        this.name = name;
        this.questions = questions;
    }

    Question getQuestion(Long index) {
        return questions.stream().filter(q -> q.id.equals(index)).collect(toList()).get(0);
    }
}

class Question {
    Long id;
    String text;
    List<String> answer = new ArrayList();

    public Question(Long id, String text, List<String> answer) {
        this.id = id;
        this.text = text;
        this.answer = answer;
    }
}
