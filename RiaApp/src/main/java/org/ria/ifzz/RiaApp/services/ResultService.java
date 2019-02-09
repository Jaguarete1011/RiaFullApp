package org.ria.ifzz.RiaApp.services;

import org.ria.ifzz.RiaApp.domain.FileData;
import org.ria.ifzz.RiaApp.domain.Result;
import org.ria.ifzz.RiaApp.repositories.ResultRepository;
import org.ria.ifzz.RiaApp.utils.CustomFileReader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;

@RestController
public class ResultService {

    @Autowired
    FileEntityService fileEntityService;

    private final CustomFileReader customFileReader;
    private final ResultRepository resultRepository;

    public ResultService(CustomFileReader customFileReader, ResultRepository resultRepository) {
        this.customFileReader = customFileReader;
        this.resultRepository = resultRepository;
    }

    public Result addResult(Long file_entity_id, Result result) throws FileNotFoundException {
        FileData fileData = fileEntityService.getById(file_entity_id).getFileData();
        result.setFileData(fileData);
        return resultRepository.save(result);
    }

    /**
     * takes file store in local disc space
     *
     * @param file uploaded file
     * @return expected List of Strings
     * @throws IOException
     */
    public List<String> getFileData(MultipartFile file) throws IOException {
        System.out.println(customFileReader.getUploadComment());

        List<String> fileLineList = customFileReader.readStoredTxtFile(file);
        List<String> expectedLineList = customFileReader.removeUnnecessaryLineFromListedFile(fileLineList);
        return expectedLineList;
    }

    /**
     * takes fileName of upload file and set specific id for each entities
     * reads given Strings List and create Result entity for each lines of given,
     *
     * @param list pre-cleaned list
     * @param file upload file
     * @return Result entities
     */
    public Result createResultFromColumnsLength(List<String> list, MultipartFile file, FileData fileData) {

        String fileName = file.getOriginalFilename();
        Result result = null;

        for (String line : list) {
            if (line.startsWith(" \tUnk")) {
                result = new Result();

                //set fileName followed by '_lineIndex'
                result.setFileName(fileName + "_" + list.indexOf(line));
                result.setFileData(fileData);

                resultRepository.save(result);
            }
        }
        return result;
    }

    /**
     * find Result entity in database by {@code}fileName_index,
     * which is created by file's fileName + _ + index, and
     * then assign results from "CCMP" table
     *
     * @param list generated by upload file
     * @param file uploaded file
     */
    public Result assignCcmpToResult(List<String> list, MultipartFile file) {

        Result result = null;
        String fileName = file.getOriginalFilename();
        for (int i = 0; i < list.size() - 1; i++) {
            List CCMP = customFileReader.getMatchingStrings(list, 3);

            int index = i + 1;
            result = resultRepository.findByFileName(fileName + "_" + index);

            result.setCcpm(CCMP.get(i).toString());
            System.out.println(" \tResult CCMP value: " + result.getCcpm());
        }
        return result;
    }

}
