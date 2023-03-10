package com.codestates.Header2Footer.question_board;

import com.codestates.Header2Footer.exception.BusinessLogicException;
import com.codestates.Header2Footer.exception.ExceptionCode;
import com.codestates.Header2Footer.member.Member;
import com.codestates.Header2Footer.member.MemberRepository;
import com.codestates.Header2Footer.utils.CustomBeanUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Transactional
@Service
public class QuestionBoardService {
    private final QuestionBoardRepository questionBoardRepository;
    private final MemberRepository memberRepository;
    private final CustomBeanUtils<QuestionBoard> beanUtils;

    public QuestionBoardService(QuestionBoardRepository questionBoardRepository, MemberRepository memberRepository, CustomBeanUtils<QuestionBoard> beanUtils) {
        this.questionBoardRepository = questionBoardRepository;
        this.memberRepository = memberRepository;
        this.beanUtils = beanUtils;
    }

    public QuestionBoard createQuestionBoard(QuestionBoard questionBoard, Long memberId){
        verifyExistsMember(memberId);

        Member member = memberRepository.getReferenceById(memberId);
        questionBoard.setMember(member);

        QuestionBoard savedQuestionBoard = questionBoardRepository.save(questionBoard);

        return savedQuestionBoard;
    }

    @Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.SERIALIZABLE)
    public QuestionBoard updateQuestionBoard(QuestionBoard questionBoard, Long memberId){
        QuestionBoard findQuestionBoard = findVerifiedQuestionBoard(questionBoard.getQuestionBoardId());

        QuestionBoard updatedQuestionBoard;

        if(findQuestionBoard.getMember().getMemberId() != memberId){ // ????????? ????????? ????????? ???????????? ????????????
            throw new BusinessLogicException(ExceptionCode.INVALID_MEMBER_STATUS);
        }else{ // ????????? ????????? ????????? ???????????????
            updatedQuestionBoard = beanUtils.copyNonNullProperties(questionBoard, findQuestionBoard);
        }

        return questionBoardRepository.save(updatedQuestionBoard);
    }

    @Transactional(readOnly = true)
    public QuestionBoard findQuestionBoard(Long questionBoardId, Long memberId){
        verifyExistsMember(memberId);
        QuestionBoard questionBoard = findVerifiedQuestionBoard(questionBoardId);
        QuestionBoard findedQuestionBoard;

        if(questionBoard.getQuestionStatus() == QuestionBoard.QuestionStatus.QUESTION_DELETE){ // ?????? ?????? ????????? ????????? ????????? ??? ??????.
            throw new BusinessLogicException(ExceptionCode.QUESTION_BOARD_REMOVED);
        }else{
            if(questionBoard.getSecretStatus() == QuestionBoard.SecretStatus.SECRET){ // ???????????? ??????
                if(questionBoard.getMember().getMemberId() == memberId || questionBoard.getMember().getEmail().equals("admin@gmail.com")){ // ???????????? ??? ????????? ??????????????? ???????????? ??????
                    findedQuestionBoard = findVerifiedQuestionBoard(questionBoardId);
                    addViews(findedQuestionBoard);
                    return findedQuestionBoard;
                }else{
                    throw new BusinessLogicException(ExceptionCode.INVALID_MEMBER_STATUS);
                }
            }else{ // ???????????? ??????
                findedQuestionBoard = findVerifiedQuestionBoard(questionBoardId);
                addViews(findedQuestionBoard);
                return findedQuestionBoard;
            }
        }
    }

    public Page<QuestionBoard> findQuestionBoards(Long memberId, int page, int size, String orderCondition){
        verifyExistsMember(memberId);
        if(orderCondition.equals("newDate")){ // ????????? ????????? ??????
            return questionBoardRepository.findWithoutDeleteBoard(PageRequest.of(page, size,
                    Sort.by("createdAt").descending()));
        } else if (orderCondition.equals("oldDate")) { // ????????? ??? ????????? ??????
            return questionBoardRepository.findWithoutDeleteBoard(PageRequest.of(page, size,
                    Sort.by("createdAt").ascending()));
        }

        // default??? ????????? ??????
        return questionBoardRepository.findWithoutDeleteBoard(PageRequest.of(page, size,
                Sort.by("createdAt").descending()));
    }

    public void deleteQuestionBoard(Long questionBoardId, Long memberId){
        QuestionBoard questionBoard = findVerifiedQuestionBoard(questionBoardId);

        if(questionBoard.getMember().getMemberId() != memberId){ // ????????? ???????????? ????????? ???????????? ????????????
            throw new BusinessLogicException(ExceptionCode.INVALID_MEMBER_STATUS);
        }else{ // ????????? ???????????? ????????? ???????????????
            if(questionBoard.getQuestionStatus() == QuestionBoard.QuestionStatus.QUESTION_DELETE){ // ?????? ????????? ????????????
                throw new BusinessLogicException(ExceptionCode.QUESTION_BOARD_REMOVED);
            }else{
                questionBoard.setQuestionStatus(QuestionBoard.QuestionStatus.QUESTION_DELETE);
            }
        }

        questionBoardRepository.save(questionBoard);
    }

    @Transactional(readOnly = true)
    public QuestionBoard findVerifiedQuestionBoard(Long questionBoardId){
        Optional<QuestionBoard> optionalQuestionBoard = questionBoardRepository.findById(questionBoardId);
        QuestionBoard findQuestionBoard =
                optionalQuestionBoard.orElseThrow(()->new BusinessLogicException(ExceptionCode.QUESTION_BOARD_NOT_FOUND));

        return findQuestionBoard;
    }

    private void verifyExistsMember(Long memberId){
        Optional<Member> member = memberRepository.findById(memberId);
        if(!member.isPresent()){
            throw new BusinessLogicException(ExceptionCode.MEMBER_NOT_FOUND);
        }
    }

    private void addViews(QuestionBoard questionBoard){ // ???????????? ???????????? ????????? 1 ??????
        questionBoard.setViews(questionBoard.getViews() + 1);
//        questionBoardRepository.save(questionBoard);
        questionBoardRepository.saveAndFlush(questionBoard);
    }
}
