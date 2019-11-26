package com.quiz.darkhold.challenge;

import com.quiz.darkhold.challenge.entity.Challenge;
import com.quiz.darkhold.challenge.entity.Options;
import com.quiz.darkhold.challenge.entity.QuestionSet;
import com.quiz.darkhold.challenge.exception.ChallengeException;
import com.quiz.darkhold.challenge.repository.ChallengeRepository;
import com.quiz.darkhold.challenge.repository.QuestionSetRepository;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class ChallengeService {
    @Autowired
    private ChallengeRepository challengeRepository;

    @Autowired
    private QuestionSetRepository questionSetRepository;

    private final Logger logger = LoggerFactory.getLogger(ChallengeService.class);

    public void readProcessAndSaveChallenge(MultipartFile upload, String title, String description)
            throws ChallengeException {
        Set<QuestionSet> questionSets = readAndProcessChallenge(upload);
        Challenge challenge = new Challenge();
        challenge.setTitle(title);
        challenge.setDescription(description);
        challenge.setQuestionSets(questionSets);
        //save challenge to db
        final Challenge savedChallenge = challengeRepository.save(challenge);
        questionSets.forEach(q -> q.setChallenge(savedChallenge));
        questionSets.forEach(questionSet -> questionSetRepository.save(questionSet));
    }

    private Set<QuestionSet> readAndProcessChallenge(MultipartFile upload) throws ChallengeException {
        Set<QuestionSet> questionSets = new HashSet<>();
        Workbook workbook = null;
        try {
            workbook = new XSSFWorkbook(upload.getInputStream());
            Sheet datatypeSheet = workbook.getSheetAt(0);
            Iterator<Row> iterator = datatypeSheet.iterator();

            while (iterator.hasNext()) {
                Row currentRow = iterator.next();
                Iterator<Cell> cellIterator = currentRow.iterator();
                QuestionSet questionSet = new QuestionSet();
                while (cellIterator.hasNext()) {
                    Cell currentCell = cellIterator.next();
                    switch (currentCell.getColumnIndex()) {
                        case 0:
                            questionSet.setQuestion(currentCell.getStringCellValue());
                            break;
                        case 1:
                            questionSet.setAnswer1(currentCell.getStringCellValue());
                            break;
                        case 2:
                            questionSet.setAnswer2(currentCell.getStringCellValue());
                            break;
                        case 3:
                            questionSet.setAnswer3(currentCell.getStringCellValue());
                            break;
                        case 4:
                            questionSet.setAnswer4(currentCell.getStringCellValue());
                            break;
                        case 5:
                            String options = currentCell.getStringCellValue();
                            String optionsList = populateOptionsFromString(options);
                            questionSet.setCorrectOptions(optionsList);
                            break;
                        default:
                            logger.error("Unknown option at " + currentCell.getColumnIndex()
                                    + ", Text is : " + currentCell.getStringCellValue());
                    }
                }
                logger.info("Current questions : " + questionSet);
                questionSets.add(questionSet);
            }
        } catch (IOException e) {
            logger.error(e.getMessage());
            throw new ChallengeException("Unable to process the file..");
        } finally {
            if (workbook != null) {
                try {
                    workbook.close();
                } catch (IOException e) {
                    logger.error(e.getMessage());
                }
            }
        }

        return questionSets;
    }

    private String populateOptionsFromString(String options) {
        if (!options.contains(",")) {
            return Options.valueOf(options).name();
        } else {
            Stream<String> option = Arrays.stream(options.split(","));
            return option.map(Options::valueOf).map(Enum::name).collect(Collectors.joining(","));
        }
    }

}
